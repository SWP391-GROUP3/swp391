/* ==========================================================================
   SWP391 - Hệ thống đánh giá chất lượng (KPI) và quản lý nhuận bút
   Schema cho SQL Server 2019 trở lên. 21 bảng, 5 luồng:
     01 Bài viết    02 Chất lượng    03 Duyệt đăng    04 Nhuận bút    05 KPI
   Luồng 03 không có bảng riêng: dùng articles, article_versions,
   article_status_logs.

   CÁCH DÙNG: mở file này trong SSMS, bấm Execute (F5).
   File re-runnable: xoá SẠCH mọi bảng trong swp391_kpi rồi tạo lại từ đầu,
   kể cả bảng của schema cũ. Chạy lại là MẤT HẾT DỮ LIỆU.
   Sửa schema thì sửa file này, commit, rồi báo cả nhóm chạy lại.

   Quy ước:
   - Tên bảng/cột snake_case tiếng Anh. Spring Boot tự map
     authorId <-> author_id, entity không cần @Column(name=...).
   - Tiền: DECIMAL(18,0) (VND, không có hào lẻ). Hệ số: DECIMAL(5,2).
   - Khoá ngoại KHÔNG dùng ON DELETE CASCADE: nhiều FK cùng trỏ về users sẽ
     làm SQL Server báo "multiple cascade paths". Không xoá user, chỉ khoá
     bằng status = 'LOCKED'.
   - SQL Server KHÔNG tự tạo index cho khoá ngoại, nên index được khai báo
     tay cho các cột hay lọc/join.
   ========================================================================== */

/* Filtered index ở royalty_lines bắt buộc 2 option này. SSMS và JDBC bật sẵn,
   sqlcmd thì không: thiếu là CREATE INDEX báo lỗi 1934. */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF DB_ID('swp391_kpi') IS NULL
    CREATE DATABASE swp391_kpi;
GO

USE swp391_kpi;
GO

/* ---------- Xoá toàn bộ bảng hiện có (cả schema cũ) ----------
   Gỡ hết khoá ngoại trước, rồi mới DROP TABLE, nên không phụ thuộc thứ tự. */
DECLARE @sql NVARCHAR(MAX) = N'';

SELECT @sql += N'ALTER TABLE ' + QUOTENAME(OBJECT_SCHEMA_NAME(parent_object_id))
             + N'.' + QUOTENAME(OBJECT_NAME(parent_object_id))
             + N' DROP CONSTRAINT ' + QUOTENAME(name) + N';'
FROM sys.foreign_keys;
EXEC sp_executesql @sql;

SET @sql = N'';
SELECT @sql += N'DROP TABLE ' + QUOTENAME(SCHEMA_NAME(schema_id))
             + N'.' + QUOTENAME(name) + N';'
FROM sys.tables;
EXEC sp_executesql @sql;
GO

/* ==========================================================================
   NỀN TẢNG (Admin)
   ========================================================================== */

/* 6 vai trò cố định, mỗi user đúng 1 vai trò. */
CREATE TABLE roles (
    id      INT IDENTITY(1,1) PRIMARY KEY,
    code    VARCHAR(20)  NOT NULL UNIQUE,   -- PHONG_VIEN, BIEN_TAP_VIEN, TRUONG_BAN, ...
    name    NVARCHAR(50) NOT NULL
);

/* Ban */
CREATE TABLE sections (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    code        VARCHAR(20)   NOT NULL UNIQUE,
    name        NVARCHAR(100) NOT NULL,
    is_active   BIT           NOT NULL DEFAULT 1
);

/* section_id: PV / BTV / Trưởng ban bắt buộc có ban, vai trò khác để NULL.
   Luật này phụ thuộc bảng roles nên kiểm ở tầng service, không CHECK được. */
CREATE TABLE users (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    username        VARCHAR(50)   NOT NULL UNIQUE,
    email           VARCHAR(150)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255)  NOT NULL,           -- BCrypt hash, KHÔNG lưu plain text
    full_name       NVARCHAR(100) NOT NULL,
    role_id         INT           NOT NULL,
    section_id      INT           NULL,
    status          VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_users_role    FOREIGN KEY (role_id)    REFERENCES roles(id),
    CONSTRAINT fk_users_section FOREIGN KEY (section_id) REFERENCES sections(id),
    CONSTRAINT ck_users_status  CHECK (status IN ('ACTIVE','LOCKED'))
);

/* Thể loại + khung hệ số phần bài viết. Không xoá thể loại đã có bài,
   chỉ tắt is_active. */
CREATE TABLE genres (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    code        VARCHAR(30)   NOT NULL UNIQUE,
    name        NVARCHAR(100) NOT NULL,
    min_factor  DECIMAL(5,2)  NOT NULL,
    max_factor  DECIMAL(5,2)  NOT NULL,
    is_active   BIT           NOT NULL DEFAULT 1,
    CONSTRAINT ck_genres_factor CHECK (min_factor > 0 AND min_factor <= max_factor)
);

/* Khung hệ số ảnh/video. 2 dòng cố định, Admin chỉ sửa min/max. */
CREATE TABLE media_factors (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    media_type  VARCHAR(10)  NOT NULL UNIQUE,
    min_factor  DECIMAL(5,2) NOT NULL,
    max_factor  DECIMAL(5,2) NOT NULL,
    updated_at  DATETIME2    NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT ck_media_factors_type   CHECK (media_type IN ('IMAGE','VIDEO')),
    CONSTRAINT ck_media_factors_factor CHECK (min_factor > 0 AND min_factor <= max_factor)
);

/* ==========================================================================
   BÀI VIẾT (luồng 01, 03)
   ========================================================================== */

/* title/content ở đây là BẢN ĐANG SOẠN, sửa thoải mái.
   Mỗi lần nộp hoặc gửi duyệt thì chụp lại vào article_versions.
   published_at quyết định bài vào kỳ nhuận bút tháng nào. */
CREATE TABLE articles (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    author_id       INT            NOT NULL,   -- tác giả chính: người tạo, nộp, rút, nộp lại
    section_id      INT            NOT NULL,   -- ban của tác giả chính TẠI LÚC TẠO BÀI
    genre_id        INT            NOT NULL,
    editor_id       INT            NULL,       -- BTV phụ trách (luồng 02)
    approved_by     INT            NULL,       -- Trưởng ban duyệt đăng (luồng 03)
    title           NVARCHAR(300)  NOT NULL,
    content         NVARCHAR(MAX)  NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    created_at      DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    updated_at      DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    submitted_at    DATETIME2      NULL,       -- lần nộp gần nhất
    published_at    DATETIME2      NULL,
    CONSTRAINT fk_articles_author   FOREIGN KEY (author_id)   REFERENCES users(id),
    CONSTRAINT fk_articles_section  FOREIGN KEY (section_id)  REFERENCES sections(id),
    CONSTRAINT fk_articles_genre    FOREIGN KEY (genre_id)    REFERENCES genres(id),
    CONSTRAINT fk_articles_editor   FOREIGN KEY (editor_id)   REFERENCES users(id),
    CONSTRAINT fk_articles_approver FOREIGN KEY (approved_by) REFERENCES users(id),
    CONSTRAINT ck_articles_status   CHECK (status IN ('DRAFT','SUBMITTED','ASSIGNED','REVISION',
                                                      'REJECTED','EDITING','PENDING_APPROVAL','PUBLISHED')),
    -- Đã PUBLISHED thì phải có ngày đăng và người duyệt, chưa PUBLISHED thì không có ngày đăng.
    CONSTRAINT ck_articles_published CHECK (
        (status =  'PUBLISHED' AND published_at IS NOT NULL AND approved_by IS NOT NULL) OR
        (status <> 'PUBLISHED' AND published_at IS NULL))
);
CREATE INDEX ix_articles_author    ON articles(author_id);
CREATE INDEX ix_articles_editor    ON articles(editor_id);
CREATE INDEX ix_articles_status    ON articles(status);
CREATE INDEX ix_articles_published ON articles(published_at);

/* Đồng tác giả, gồm cả tác giả chính. Tổng share_percent của 1 bài = 100:
   luật trên nhiều dòng nên kiểm ở tầng service. */
CREATE TABLE article_authors (
    article_id      INT          NOT NULL,
    user_id         INT          NOT NULL,
    share_percent   DECIMAL(5,2) NOT NULL,
    PRIMARY KEY (article_id, user_id),
    CONSTRAINT fk_article_authors_article FOREIGN KEY (article_id) REFERENCES articles(id),
    CONSTRAINT fk_article_authors_user    FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT ck_article_authors_share   CHECK (share_percent > 0 AND share_percent <= 100)
);
CREATE INDEX ix_article_authors_user ON article_authors(user_id);

/* Bản chụp lúc nộp / lúc gửi duyệt. Chỉ INSERT, không UPDATE, không DELETE.
   SUBMISSION = bản PV nộp | EDITED = bản BTV gửi Trưởng ban duyệt (luồng 03). */
CREATE TABLE article_versions (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    article_id  INT            NOT NULL,
    version_no  INT            NOT NULL,
    kind        VARCHAR(20)    NOT NULL,
    title       NVARCHAR(300)  NOT NULL,
    content     NVARCHAR(MAX)  NOT NULL,
    created_by  INT            NOT NULL,
    created_at  DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_article_versions_article FOREIGN KEY (article_id) REFERENCES articles(id),
    CONSTRAINT fk_article_versions_creator FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT uq_article_versions_no      UNIQUE (article_id, version_no),
    CONSTRAINT ck_article_versions_kind    CHECK (kind IN ('SUBMISSION','EDITED'))
);

/* Ảnh/video. author_id là người nhận nhuận bút phần này, có thể khác
   tác giả bài. version_id NULL = đang soạn, chưa gắn vào bản nộp nào. */
CREATE TABLE article_attachments (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    article_id  INT            NOT NULL,
    version_id  INT            NULL,
    author_id   INT            NOT NULL,
    media_type  VARCHAR(10)    NOT NULL,
    url         NVARCHAR(500)  NOT NULL,
    caption     NVARCHAR(300)  NULL,
    created_at  DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_article_attachments_article FOREIGN KEY (article_id) REFERENCES articles(id),
    CONSTRAINT fk_article_attachments_version FOREIGN KEY (version_id) REFERENCES article_versions(id),
    CONSTRAINT fk_article_attachments_author  FOREIGN KEY (author_id)  REFERENCES users(id),
    CONSTRAINT ck_article_attachments_type    CHECK (media_type IN ('IMAGE','VIDEO'))
);
CREATE INDEX ix_article_attachments_article ON article_attachments(article_id);

/* ==========================================================================
   CHẤT LƯỢNG (luồng 02)
   ========================================================================== */

/* Tổng weight_percent các tiêu chí is_active = 1 phải bằng 100:
   kiểm ở tầng service khi Admin lưu. */
CREATE TABLE quality_criteria (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    name            NVARCHAR(100) NOT NULL,
    weight_percent  DECIMAL(5,2)  NOT NULL,
    is_active       BIT           NOT NULL DEFAULT 1,
    CONSTRAINT ck_quality_criteria_weight CHECK (weight_percent > 0 AND weight_percent <= 100)
);

/* Mỗi bài 1 phiếu, chấm trên bản PV nộp lần đầu BTV mở bài. */
CREATE TABLE quality_reviews (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    article_id   INT            NOT NULL UNIQUE,
    version_id   INT            NOT NULL,
    editor_id    INT            NOT NULL,
    total_score  DECIMAL(4,2)   NOT NULL,   -- 0-10, = tổng (score * weight_percent / 100)
    comment      NVARCHAR(1000) NULL,
    created_at   DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_quality_reviews_article FOREIGN KEY (article_id) REFERENCES articles(id),
    CONSTRAINT fk_quality_reviews_version FOREIGN KEY (version_id) REFERENCES article_versions(id),
    CONSTRAINT fk_quality_reviews_editor  FOREIGN KEY (editor_id)  REFERENCES users(id),
    CONSTRAINT ck_quality_reviews_score   CHECK (total_score BETWEEN 0 AND 10)
);

/* weight_percent CHỤP LẠI trọng số lúc chấm. Admin đổi trọng số sau đó thì
   phiếu cũ vẫn tính lại ra đúng total_score cũ. */
CREATE TABLE quality_review_items (
    review_id       INT          NOT NULL,
    criterion_id    INT          NOT NULL,
    weight_percent  DECIMAL(5,2) NOT NULL,
    score           DECIMAL(4,2) NOT NULL,
    PRIMARY KEY (review_id, criterion_id),
    CONSTRAINT fk_quality_review_items_review    FOREIGN KEY (review_id)    REFERENCES quality_reviews(id),
    CONSTRAINT fk_quality_review_items_criterion FOREIGN KEY (criterion_id) REFERENCES quality_criteria(id),
    CONSTRAINT ck_quality_review_items_score     CHECK (score BETWEEN 0 AND 10)
);

/* ==========================================================================
   NHUẬN BÚT (luồng 04)
   ========================================================================== */

/* Tiền cho 1 hệ số. Đơn giá áp dụng cho kỳ = dòng có effective_from lớn nhất
   mà vẫn <= ngày đầu tháng của kỳ. */
CREATE TABLE unit_prices (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    amount          DECIMAL(18,0) NOT NULL,
    effective_from  DATE          NOT NULL UNIQUE,
    created_by      INT           NOT NULL,   -- Kế toán
    created_at      DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_unit_prices_creator FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT ck_unit_prices_amount  CHECK (amount > 0)
);

/* Kỳ nhuận bút theo tháng.
   OPEN -> CLOSED (TKTS chốt) -> LOCKED (Kế toán khoá) -> PAID (đã chi hết).
   Từ LOCKED trở đi mọi số liệu của kỳ là BẤT BIẾN. */
CREATE TABLE royalty_periods (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    period_year   SMALLINT    NOT NULL,
    period_month  TINYINT     NOT NULL,
    status        VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    closed_by     INT         NULL,
    closed_at     DATETIME2   NULL,
    locked_by     INT         NULL,
    locked_at     DATETIME2   NULL,
    CONSTRAINT fk_royalty_periods_closer FOREIGN KEY (closed_by) REFERENCES users(id),
    CONSTRAINT fk_royalty_periods_locker FOREIGN KEY (locked_by) REFERENCES users(id),
    CONSTRAINT uq_royalty_periods        UNIQUE (period_year, period_month),
    CONSTRAINT ck_royalty_periods_month  CHECK (period_month BETWEEN 1 AND 12),
    CONSTRAINT ck_royalty_periods_status CHECK (status IN ('OPEN','CLOSED','LOCKED','PAID')),
    -- Qua mốc nào thì phải ghi ai làm và lúc nào.
    CONSTRAINT ck_royalty_periods_closed CHECK (status = 'OPEN'
                                                OR (closed_by IS NOT NULL AND closed_at IS NOT NULL)),
    CONSTRAINT ck_royalty_periods_locked CHECK (status IN ('OPEN','CLOSED')
                                                OR (locked_by IS NOT NULL AND locked_at IS NOT NULL))
);

/* Phiếu chấm nhuận bút phần bài viết, mỗi bài 1 phiếu.
   period_id PHẢI là kỳ cùng tháng với articles.published_at và đang OPEN:
   kiểm ở tầng service (không CHECK chéo bảng được).
   unit_price và amount để NULL cho tới lúc khoá kỳ thì điền cùng lúc. */
CREATE TABLE royalty_gradings (
    id                INT IDENTITY(1,1) PRIMARY KEY,
    article_id        INT           NOT NULL UNIQUE,
    period_id         INT           NOT NULL,
    graded_by         INT           NOT NULL,   -- TKTS
    suggested_factor  DECIMAL(5,2)  NULL,       -- gợi ý từ điểm chất lượng
    factor            DECIMAL(5,2)  NOT NULL,   -- TKTS quyết, trong khung thể loại
    unit_price        DECIMAL(18,0) NULL,
    amount            DECIMAL(18,0) NULL,       -- = factor * unit_price
    graded_at         DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_royalty_gradings_article FOREIGN KEY (article_id) REFERENCES articles(id),
    CONSTRAINT fk_royalty_gradings_period  FOREIGN KEY (period_id)  REFERENCES royalty_periods(id),
    CONSTRAINT fk_royalty_gradings_grader  FOREIGN KEY (graded_by)  REFERENCES users(id),
    CONSTRAINT ck_royalty_gradings_factor  CHECK (factor > 0),
    CONSTRAINT ck_royalty_gradings_amount  CHECK (
        (unit_price IS NULL     AND amount IS NULL) OR
        (unit_price IS NOT NULL AND amount IS NOT NULL AND amount >= 0))
);
CREATE INDEX ix_royalty_gradings_period ON royalty_gradings(period_id);

/* Hệ số từng ảnh/video (Iter 3). Attachment phải thuộc đúng bài của grading:
   kiểm ở tầng service. */
CREATE TABLE royalty_media_items (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    grading_id     INT           NOT NULL,
    attachment_id  INT           NOT NULL UNIQUE,
    factor         DECIMAL(5,2)  NOT NULL,   -- trong khung media_factors
    amount         DECIMAL(18,0) NULL,       -- điền lúc khoá kỳ
    CONSTRAINT fk_royalty_media_items_grading    FOREIGN KEY (grading_id)    REFERENCES royalty_gradings(id),
    CONSTRAINT fk_royalty_media_items_attachment FOREIGN KEY (attachment_id) REFERENCES article_attachments(id),
    CONSTRAINT ck_royalty_media_items_factor     CHECK (factor > 0),
    CONSTRAINT ck_royalty_media_items_amount     CHECK (amount IS NULL OR amount >= 0)
);
CREATE INDEX ix_royalty_media_items_grading ON royalty_media_items(grading_id);

/* Tiền của từng người, sinh ra khi khoá kỳ.
   media_item_id NULL = phần bài viết, chia theo article_authors.share_percent.
   media_item_id có   = phần ảnh/video, trả cho tác giả ảnh.
   Tổng các dòng tiền bài của 1 grading phải bằng ĐÚNG grading.amount,
   không lệch 1 đồng: package royalty lo phần chia dư. */
CREATE TABLE royalty_lines (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    period_id       INT           NOT NULL,
    user_id         INT           NOT NULL,
    grading_id      INT           NOT NULL,
    media_item_id   INT           NULL,
    amount          DECIMAL(18,0) NOT NULL,
    created_at      DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_royalty_lines_period  FOREIGN KEY (period_id)     REFERENCES royalty_periods(id),
    CONSTRAINT fk_royalty_lines_user    FOREIGN KEY (user_id)       REFERENCES users(id),
    CONSTRAINT fk_royalty_lines_grading FOREIGN KEY (grading_id)    REFERENCES royalty_gradings(id),
    CONSTRAINT fk_royalty_lines_media   FOREIGN KEY (media_item_id) REFERENCES royalty_media_items(id),
    CONSTRAINT ck_royalty_lines_amount  CHECK (amount >= 0)
);
CREATE INDEX ix_royalty_lines_period_user ON royalty_lines(period_id, user_id);
-- Chặn ghi trùng khi khoá kỳ bị chạy lại: mỗi người 1 dòng tiền bài / grading,
-- mỗi ảnh/video đúng 1 dòng.
CREATE UNIQUE INDEX uq_royalty_lines_article ON royalty_lines(grading_id, user_id) WHERE media_item_id IS NULL;
CREATE UNIQUE INDEX uq_royalty_lines_media   ON royalty_lines(media_item_id)       WHERE media_item_id IS NOT NULL;

/* Chi trả: mỗi người 1 dòng mỗi kỳ.
   total_amount = tổng royalty_lines của người đó trong kỳ, chụp lúc khoá. */
CREATE TABLE royalty_payments (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    period_id     INT           NOT NULL,
    user_id       INT           NOT NULL,
    total_amount  DECIMAL(18,0) NOT NULL,
    status        VARCHAR(10)   NOT NULL DEFAULT 'UNPAID',
    paid_by       INT           NULL,      -- Kế toán
    paid_at       DATETIME2     NULL,
    CONSTRAINT fk_royalty_payments_period FOREIGN KEY (period_id) REFERENCES royalty_periods(id),
    CONSTRAINT fk_royalty_payments_user   FOREIGN KEY (user_id)   REFERENCES users(id),
    CONSTRAINT fk_royalty_payments_payer  FOREIGN KEY (paid_by)   REFERENCES users(id),
    CONSTRAINT uq_royalty_payments        UNIQUE (period_id, user_id),
    CONSTRAINT ck_royalty_payments_amount CHECK (total_amount >= 0),
    CONSTRAINT ck_royalty_payments_status CHECK (
        (status = 'UNPAID' AND paid_by IS NULL     AND paid_at IS NULL) OR
        (status = 'PAID'   AND paid_by IS NOT NULL AND paid_at IS NOT NULL))
);

/* ==========================================================================
   KPI (luồng 05)
   ========================================================================== */

/* Chỉ tiêu tháng. Kết quả thực tế TÍNH từ articles + quality_reviews,
   không lưu, để không bao giờ lệch với dữ liệu gốc. */
CREATE TABLE kpi_targets (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    reporter_id     INT          NOT NULL,
    target_year     SMALLINT     NOT NULL,
    target_month    TINYINT      NOT NULL,
    min_published   INT          NOT NULL,   -- số bài xuất bản tối thiểu
    min_avg_score   DECIMAL(4,2) NOT NULL,   -- điểm chất lượng TB tối thiểu
    set_by          INT          NOT NULL,   -- Trưởng ban
    created_at      DATETIME2    NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_kpi_targets_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_kpi_targets_setter   FOREIGN KEY (set_by)      REFERENCES users(id),
    CONSTRAINT uq_kpi_targets          UNIQUE (reporter_id, target_year, target_month),
    CONSTRAINT ck_kpi_targets_month    CHECK (target_month BETWEEN 1 AND 12),
    CONSTRAINT ck_kpi_targets_min      CHECK (min_published >= 0 AND min_avg_score BETWEEN 0 AND 10)
);

/* ==========================================================================
   BẢNG PHỤ
   ========================================================================== */

/* Lịch sử xử lý bài. Chỉ INSERT. note bắt buộc khi trả bài (REVISION)
   hoặc từ chối (REJECTED): kiểm ở tầng service. */
CREATE TABLE article_status_logs (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    article_id   INT            NOT NULL,
    actor_id     INT            NOT NULL,
    action       VARCHAR(20)    NOT NULL,   -- CREATE, SUBMIT, ASSIGN, RETURN, REJECT, SEND_APPROVAL, PUBLISH, ...
    from_status  VARCHAR(20)    NULL,       -- NULL ở lần tạo bài
    to_status    VARCHAR(20)    NOT NULL,
    note         NVARCHAR(1000) NULL,
    created_at   DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_article_status_logs_article FOREIGN KEY (article_id) REFERENCES articles(id),
    CONSTRAINT fk_article_status_logs_actor   FOREIGN KEY (actor_id)   REFERENCES users(id)
);
CREATE INDEX ix_article_status_logs_article ON article_status_logs(article_id, created_at);

CREATE TABLE notifications (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    user_id     INT           NOT NULL,
    article_id  INT           NULL,        -- bấm vào thông báo thì mở bài này
    message     NVARCHAR(500) NOT NULL,
    is_read     BIT           NOT NULL DEFAULT 0,
    created_at  DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_notifications_user    FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT fk_notifications_article FOREIGN KEY (article_id) REFERENCES articles(id)
);
CREATE INDEX ix_notifications_user ON notifications(user_id, is_read);
GO

PRINT N'Tạo schema thành công (21 bảng). Chạy tiếp data.sql để có dữ liệu mẫu.';
GO
