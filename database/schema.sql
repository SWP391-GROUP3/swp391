-- noinspection SqlNoDataSourceInspectionForFile
/* ==========================================================================
   SWP391 - Hệ thống đánh giá chất lượng bài báo và quản lý nhuận bút
   Schema cho SQL Server 2019 trở lên. Khớp ERD tổng thể (SWP391_ERD_TongThe):
   40 bảng, chia theo package của server:
     admin       roles, sections, users, genres, media_factors, system_parameters
     article     BF-01  articles, article_authors, article_versions,
                        article_attachments, article_status_logs,
                        duplication_checks, duplication_matches
     assessment  BF-02  assignments, category_change_logs, criteria_set_versions,
                        criteria, scoring_attempts, scoring_attempt_details,
                        score_approvals
     appeal      BF-03  appeals, appeal_criteria, appeal_resolutions,
                        appeal_rule_configs, cross_review_batches,
                        cross_review_tasks, reviewer_deviation_indices
     royalty     BF-04  royalty_rates, events, royalty_periods, royalty_gradings,
                        royalty_media_items, royalty_payments, royalty_lines
     kpi         BF-05  kpi_targets, kpi_snapshots, csv_import_logs,
                        reader_interactions
     common             notifications, audit_logs

   CÁCH DÙNG: mở file này trong SSMS, bấm Execute (F5).
   File re-runnable: xoá SẠCH mọi bảng trong swp391_kpi rồi tạo lại từ đầu,
   kể cả bảng của schema cũ. Chạy lại là MẤT HẾT DỮ LIỆU.
   Sửa schema thì sửa file này, commit, rồi báo cả nhóm chạy lại.

   Quy ước:
   - Tên bảng/cột snake_case tiếng Anh. Spring Boot tự map
     authorId <-> author_id, entity không cần @Column(name=...).
   - Tiền: DECIMAL(18,0) (VND). Hệ số, điểm, phần trăm: DECIMAL(5,2).
   - Điểm chất lượng thang 0-100 (hệ số nhuận bút = base_factor x điểm / 100, BR-RY-01).
   - Mọi cột *_rate, *_percent lưu dạng PHẦN TRĂM: 100.00 = giữ nguyên,
     70.00 = còn 70%. Khi nhân thì chia 100.
   - Khoá ngoại KHÔNG dùng ON DELETE CASCADE: nhiều FK cùng trỏ về users sẽ
     làm SQL Server báo "multiple cascade paths". Không xoá user, chỉ khoá
     bằng status = 'LOCKED'. Danh mục đã dùng thì tắt is_active, không xoá.
   - SQL Server KHÔNG tự tạo index cho khoá ngoại, nên index được khai báo
     tay cho các cột hay lọc/join.
   - Chỗ ghi [ĐỀ XUẤT]: ERD không ghi giá trị, nhóm có thể đổi.
   ========================================================================== */

/* Filtered index (có WHERE) bắt buộc 2 option này. SSMS và JDBC bật sẵn,
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
   ADMIN - nền tảng và cấu hình
   ========================================================================== */

/* Vai trò cố định, mỗi user đúng 1 vai trò. */
CREATE TABLE roles (
    id      INT IDENTITY(1,1) PRIMARY KEY,
    code    VARCHAR(20)  NOT NULL UNIQUE,   -- REPORTER, EDITOR, HEAD_OF_DEPARTMENT, ...
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

/* Thể loại bài. base_factor = hệ số chuẩn: hệ số nhuận bút của bài
   = base_factor x điểm / 100, hệ thống tự tính, TKTS không nâng (BR-RY-01).
   nd_group = nhóm thể loại theo khung Nghị định 18/2014 (thay bằng
   NĐ 287/2026 từ 01/09/2026). */
CREATE TABLE genres (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    code         VARCHAR(30)   NOT NULL UNIQUE,
    name         NVARCHAR(100) NOT NULL,
    nd_group     NVARCHAR(100) NOT NULL,
    base_factor  DECIMAL(5,2)  NOT NULL,
    is_active    BIT           NOT NULL DEFAULT 1,
    CONSTRAINT ck_genres_factor CHECK (base_factor > 0)
);

/* Hệ số ảnh/video theo loại tác phẩm (BR-RY-04: dùng hệ số cố định factor).
   media_kind [ĐỀ XUẤT]: PHOTO (ảnh tác nghiệp), GRAPHIC (đồ hoạ/infographic),
   VIDEO. article_attachments.media_kind lấy giá trị trong cột này. */
CREATE TABLE media_factors (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    media_kind  VARCHAR(20)  NOT NULL UNIQUE,
    factor      DECIMAL(5,2) NOT NULL,
    CONSTRAINT ck_media_factors_factor CHECK (factor > 0)
);

/* Tham số hệ thống dạng key-value: số ngày xác nhận đồng tác giả, số tác giả
   tối đa, tỷ lệ tính chất bài, đơn vị làm tròn, hạn chi nhuận bút...
   Code đọc theo param_key, KHÔNG viết cứng số trong Java.
   flow = luồng dùng tham số (BF-01 ... BF-05), NULL = dùng chung. */
CREATE TABLE system_parameters (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    param_key    VARCHAR(50)   NOT NULL UNIQUE,
    param_value  NVARCHAR(200) NOT NULL,
    unit         NVARCHAR(20)  NULL,
    flow         VARCHAR(10)   NULL,
    updated_by   INT           NULL,
    updated_at   DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_system_parameters_updater FOREIGN KEY (updated_by) REFERENCES users(id)
);

/* ==========================================================================
   ROYALTY (phần danh mục cần có trước bài viết)
   ========================================================================== */

/* Sự kiện lớn (bầu cử, bão lũ...). Nhiều bài cùng sự kiện thì bài xếp sau
   thứ k_limit trong cửa sổ window_days bị giảm hệ số bão hoà, trừ bài có
   điểm >= exempt_score. */
CREATE TABLE events (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    name          NVARCHAR(200) NOT NULL,
    start_date    DATE          NOT NULL,
    end_date      DATE          NOT NULL,
    window_days   INT           NOT NULL DEFAULT 7,
    k_limit       INT           NOT NULL DEFAULT 10,
    exempt_score  DECIMAL(5,2)  NOT NULL DEFAULT 90,
    is_active     BIT           NOT NULL DEFAULT 1,
    created_by    INT           NOT NULL,
    CONSTRAINT fk_events_creator FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT ck_events_dates   CHECK (start_date <= end_date),
    CONSTRAINT ck_events_limits  CHECK (window_days > 0 AND k_limit > 0
                                        AND exempt_score BETWEEN 0 AND 100)
);

/* ==========================================================================
   ARTICLE - BF-01 Viết & nộp bài
   ========================================================================== */

/* title/content là BẢN ĐANG SOẠN, sửa thoải mái.
   Mỗi lần nộp hoặc BTV biên tập thì chụp lại vào article_versions.
   declared_category_id = thể loại PV khai; category_id = thể loại áp dụng
   (lúc tạo bằng declared, BTV đổi thì ghi category_change_logs).
   Rút bài: SUBMITTED -> DRAFT, ghi withdrawn_at.
   Bài có đồng tác giả: nộp -> PENDING_CONFIRMATION, mọi đồng tác giả
   ACCEPTED -> SUBMITTED; 1 người từ chối hoặc quá hạn 3 ngày -> DRAFT
   (BR-IN-06). Bài chỉ 1 tác giả: nộp -> SUBMITTED luôn.
   saturation_rank / saturation_rate: thứ hạng và tỷ lệ bão hoà khi bài
   thuộc sự kiện (event_id), NULL nếu không thuộc sự kiện nào. */
CREATE TABLE articles (
    id                    INT IDENTITY(1,1) PRIMARY KEY,
    author_id             INT            NOT NULL,   -- tác giả chính: người tạo, nộp, rút, nộp lại
    section_id            INT            NOT NULL,   -- ban của tác giả chính TẠI LÚC TẠO BÀI
    declared_category_id  INT            NOT NULL,
    category_id           INT            NOT NULL,
    event_id              INT            NULL,
    approved_by           INT            NULL,       -- Trưởng ban duyệt đăng
    title                 NVARCHAR(300)  NOT NULL,
    content               NVARCHAR(MAX)  NULL,
    declared_nature       VARCHAR(20)    NOT NULL DEFAULT 'ORIGINAL',
    status                VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    saturation_rank       INT            NULL,
    saturation_rate       DECIMAL(5,2)   NULL,
    submitted_at          DATETIME2      NULL,       -- lần nộp gần nhất
    withdrawn_at          DATETIME2      NULL,       -- lần rút gần nhất
    published_at          DATETIME2      NULL,
    created_at            DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    updated_at            DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_articles_author            FOREIGN KEY (author_id)            REFERENCES users(id),
    CONSTRAINT fk_articles_section           FOREIGN KEY (section_id)           REFERENCES sections(id),
    CONSTRAINT fk_articles_declared_category FOREIGN KEY (declared_category_id) REFERENCES genres(id),
    CONSTRAINT fk_articles_category          FOREIGN KEY (category_id)          REFERENCES genres(id),
    CONSTRAINT fk_articles_event             FOREIGN KEY (event_id)             REFERENCES events(id),
    CONSTRAINT fk_articles_approver          FOREIGN KEY (approved_by)          REFERENCES users(id),
    CONSTRAINT ck_articles_nature  CHECK (declared_nature IN ('ORIGINAL','TRANSLATION','COMPILATION','REPOST')),
    CONSTRAINT ck_articles_status  CHECK (status IN ('DRAFT','PENDING_CONFIRMATION','SUBMITTED','ASSIGNED',
                                                     'REVISION_REQUESTED','REJECTED','PENDING_APPROVAL',
                                                     'SCORE_PUBLISHED','PUBLISHED')),
    CONSTRAINT ck_articles_saturation CHECK (
        (saturation_rank IS NULL AND saturation_rate IS NULL) OR
        (saturation_rank >= 1 AND saturation_rate > 0 AND saturation_rate <= 100)),
    -- Đã PUBLISHED thì phải có ngày đăng và người duyệt, chưa PUBLISHED thì không có ngày đăng.
    CONSTRAINT ck_articles_published CHECK (
        (status =  'PUBLISHED' AND published_at IS NOT NULL AND approved_by IS NOT NULL) OR
        (status <> 'PUBLISHED' AND published_at IS NULL))
);
CREATE INDEX ix_articles_author    ON articles(author_id);
CREATE INDEX ix_articles_status    ON articles(status);
CREATE INDEX ix_articles_event     ON articles(event_id);
CREATE INDEX ix_articles_published ON articles(published_at);

/* Đồng tác giả, gồm cả tác giả chính (dòng của tác giả chính ACCEPTED ngay).
   Tối đa 5 tác giả, tổng share_percent = 100: luật nhiều dòng, kiểm ở service.
   Đồng tác giả phải xác nhận trước confirm_deadline (3 ngày), quá hạn
   job đổi sang EXPIRED. reject_reason bắt buộc khi REJECTED. */
CREATE TABLE article_authors (
    article_id        INT            NOT NULL,
    user_id           INT            NOT NULL,
    share_percent     DECIMAL(5,2)   NOT NULL,
    confirm_status    VARCHAR(10)    NOT NULL DEFAULT 'PENDING',
    confirm_deadline  DATETIME2      NULL,
    responded_at      DATETIME2      NULL,
    reject_reason     NVARCHAR(500)  NULL,
    PRIMARY KEY (article_id, user_id),
    CONSTRAINT fk_article_authors_article FOREIGN KEY (article_id) REFERENCES articles(id),
    CONSTRAINT fk_article_authors_user    FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT ck_article_authors_share   CHECK (share_percent > 0 AND share_percent <= 100),
    CONSTRAINT ck_article_authors_status  CHECK (confirm_status IN ('PENDING','ACCEPTED','REJECTED','EXPIRED')),
    CONSTRAINT ck_article_authors_reject  CHECK (confirm_status <> 'REJECTED' OR reject_reason IS NOT NULL)
);
CREATE INDEX ix_article_authors_user ON article_authors(user_id);

/* Bản chụp nội dung. Chỉ INSERT, không UPDATE, không DELETE.
   kind: SUBMISSION = bản PV nộp | EDITED = bản BTV biên tập.
   change_type [ĐỀ XUẤT]: INITIAL (bản đầu) | MINOR (sửa nhỏ) | MAJOR (sửa
   lớn, phải chấm lại điểm). */
CREATE TABLE article_versions (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    article_id   INT            NOT NULL,
    version_no   INT            NOT NULL,
    kind         VARCHAR(20)    NOT NULL,
    title        NVARCHAR(300)  NOT NULL,
    content      NVARCHAR(MAX)  NOT NULL,
    change_type  VARCHAR(10)    NOT NULL DEFAULT 'INITIAL',
    created_by   INT            NOT NULL,
    created_at   DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_article_versions_article FOREIGN KEY (article_id) REFERENCES articles(id),
    CONSTRAINT fk_article_versions_creator FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT uq_article_versions_no      UNIQUE (article_id, version_no),
    CONSTRAINT ck_article_versions_kind    CHECK (kind IN ('SUBMISSION','EDITED')),
    CONSTRAINT ck_article_versions_change  CHECK (change_type IN ('INITIAL','MINOR','MAJOR'))
);

/* Ảnh/video. author_id là người nhận nhuận bút phần này, PHẢI là một trong
   các tác giả của bài (có dòng trong article_authors, BR-IN-05): kiểm ở service.
   version_id NULL = đang soạn, chưa gắn vào bản nộp nào.
   File lưu trên ổ đĩa server, DB chỉ giữ đường dẫn (url) và mã băm.
   Kiểm trùng ảnh: file_hash (SHA-256) trùng y hệt, dhash (64 bit, dạng hex)
   gần giống khi distance_bits <= DHASH_MAX_BITS (5 bit, system_parameters).
   hash_result: NULL = chưa kiểm | NEW = không trùng | DUPLICATE = trùng
   file_hash | SIMILAR = dhash gần giống. DUPLICATE/SIMILAR thì
   matched_attachment_id trỏ ảnh bị trùng. Trùng CHƯA chắc mất tiền: chỉ khi
   ảnh bị trùng đã từng được trả nhuận bút mới là tái sử dụng, không tính
   tiền (BR-IN-05, BR-RY-04). Luồng royalty kiểm việc này lúc chấm ảnh (UC-48).
   [ĐỀ XUẤT] "đã từng được trả" = ảnh bị trùng đã có dòng trong royalty_media_items.
   external_source khác NULL = ảnh lấy từ nguồn ngoài, không tính tiền. */
CREATE TABLE article_attachments (
    id                     INT IDENTITY(1,1) PRIMARY KEY,
    article_id             INT            NOT NULL,
    version_id             INT            NULL,
    author_id              INT            NOT NULL,
    media_type             VARCHAR(10)    NOT NULL,   -- định dạng file: IMAGE / VIDEO
    media_kind             VARCHAR(20)    NOT NULL,   -- loại tác phẩm, tra media_factors
    external_source        NVARCHAR(200)  NULL,
    url                    NVARCHAR(500)  NOT NULL,
    file_hash              VARCHAR(64)    NOT NULL,
    dhash                  VARCHAR(16)    NULL,       -- chỉ có với ảnh
    hash_result            VARCHAR(10)    NULL,
    distance_bits          TINYINT        NULL,       -- khoảng cách Hamming 0-64
    matched_attachment_id  INT            NULL,
    caption                NVARCHAR(300)  NULL,
    created_at             DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_article_attachments_article FOREIGN KEY (article_id)            REFERENCES articles(id),
    CONSTRAINT fk_article_attachments_version FOREIGN KEY (version_id)            REFERENCES article_versions(id),
    CONSTRAINT fk_article_attachments_author  FOREIGN KEY (author_id)             REFERENCES users(id),
    CONSTRAINT fk_article_attachments_matched FOREIGN KEY (matched_attachment_id) REFERENCES article_attachments(id),
    CONSTRAINT fk_article_attachments_kind    FOREIGN KEY (media_kind)            REFERENCES media_factors(media_kind),
    CONSTRAINT ck_article_attachments_type    CHECK (media_type IN ('IMAGE','VIDEO')),
    CONSTRAINT ck_article_attachments_result  CHECK (hash_result IN ('NEW','DUPLICATE','SIMILAR')),
    CONSTRAINT ck_article_attachments_bits    CHECK (distance_bits <= 64),
    CONSTRAINT ck_article_attachments_match   CHECK (
        (hash_result IN ('DUPLICATE','SIMILAR') AND matched_attachment_id IS NOT NULL) OR
        ((hash_result IS NULL OR hash_result = 'NEW') AND matched_attachment_id IS NULL))
);
CREATE INDEX ix_article_attachments_article ON article_attachments(article_id);
CREATE INDEX ix_article_attachments_hash    ON article_attachments(file_hash);

/* Kiểm trùng nội dung bài với các bài đã có, chạy trên 1 bản nộp.
   result theo max_overlap_percent (BR-IN-04, ngưỡng lấy từ system_parameters):
     NORMAL        dưới DUP_WARN_PERCENT (20%)
     NEED_EXPLAIN  từ 20% đến DUP_BLOCK_PERCENT (50%), hoặc trên 50% nhưng
                   tính chất khác ORIGINAL: PV phải nhập explanation
     BLOCKED       trên 50% và tính chất ORIGINAL: chặn nộp bài */
CREATE TABLE duplication_checks (
    id                   INT IDENTITY(1,1) PRIMARY KEY,
    article_id           INT             NOT NULL,
    version_id           INT             NOT NULL,
    max_overlap_percent  DECIMAL(5,2)    NOT NULL,
    result               VARCHAR(15)     NOT NULL,
    explanation          NVARCHAR(1000)  NULL,
    checked_at           DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_duplication_checks_article FOREIGN KEY (article_id) REFERENCES articles(id),
    CONSTRAINT fk_duplication_checks_version FOREIGN KEY (version_id) REFERENCES article_versions(id),
    CONSTRAINT ck_duplication_checks_percent CHECK (max_overlap_percent BETWEEN 0 AND 100),
    CONSTRAINT ck_duplication_checks_result  CHECK (result IN ('NORMAL','NEED_EXPLAIN','BLOCKED'))
);
CREATE INDEX ix_duplication_checks_article ON duplication_checks(article_id);

/* Từng bài bị trùng trong 1 lần kiểm. passages = các đoạn trùng. */
CREATE TABLE duplication_matches (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    check_id            INT            NOT NULL,
    matched_article_id  INT            NOT NULL,
    overlap_percent     DECIMAL(5,2)   NOT NULL,
    passages            NVARCHAR(MAX)  NULL,
    CONSTRAINT fk_duplication_matches_check   FOREIGN KEY (check_id)           REFERENCES duplication_checks(id),
    CONSTRAINT fk_duplication_matches_article FOREIGN KEY (matched_article_id) REFERENCES articles(id),
    CONSTRAINT uq_duplication_matches         UNIQUE (check_id, matched_article_id),
    CONSTRAINT ck_duplication_matches_percent CHECK (overlap_percent BETWEEN 0 AND 100)
);

/* Lịch sử xử lý bài. Chỉ INSERT. note bắt buộc khi trả bài
   (REVISION_REQUESTED) hoặc từ chối (REJECTED): kiểm ở tầng service.
   actor_id NULL = job hệ thống tự chạy (vd quá hạn 3 ngày xác nhận đồng
   tác giả, UC-12 A2), giống audit_logs. */
CREATE TABLE article_status_logs (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    article_id   INT            NOT NULL,
    actor_id     INT            NULL,
    action       VARCHAR(20)    NOT NULL,   -- CREATE, SUBMIT, WITHDRAW, ASSIGN, RETURN, REJECT, PUBLISH, ...
    from_status  VARCHAR(20)    NULL,       -- NULL ở lần tạo bài
    to_status    VARCHAR(20)    NOT NULL,
    note         NVARCHAR(1000) NULL,
    created_at   DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_article_status_logs_article FOREIGN KEY (article_id) REFERENCES articles(id),
    CONSTRAINT fk_article_status_logs_actor   FOREIGN KEY (actor_id)   REFERENCES users(id)
);
CREATE INDEX ix_article_status_logs_article ON article_status_logs(article_id, created_at);

/* ==========================================================================
   COMMON - dùng chung
   ========================================================================== */

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

/* Nhật ký thay đổi dữ liệu quan trọng (cấu hình, tiền, điểm). Chỉ INSERT.
   actor_id NULL = job hệ thống tự chạy. old_value/new_value lưu dạng JSON. */
CREATE TABLE audit_logs (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    actor_id     INT            NULL,
    action_type  VARCHAR(30)    NOT NULL,   -- CREATE, UPDATE, LOCK, RETURN, ...
    entity_type  VARCHAR(50)    NOT NULL,   -- tên bảng, vd royalty_periods
    entity_id    INT            NOT NULL,
    old_value    NVARCHAR(MAX)  NULL,
    new_value    NVARCHAR(MAX)  NULL,
    created_at   DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users(id)
);
CREATE INDEX ix_audit_logs_entity ON audit_logs(entity_type, entity_id);

/* ==========================================================================
   ASSESSMENT - BF-02 Phân công & chấm điểm chất lượng
   ========================================================================== */

/* Mỗi bài tối đa 1 phân công (ERD 1 - 0..1). Đổi BTV thì UPDATE dòng này.
   was_suggested = 1 nếu Trưởng ban chọn đúng BTV hệ thống gợi ý. */
CREATE TABLE assignments (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    article_id     INT        NOT NULL UNIQUE,
    editor_id      INT        NOT NULL,
    assigned_by    INT        NOT NULL,   -- Trưởng ban
    assigned_at    DATETIME2  NOT NULL DEFAULT SYSDATETIME(),
    was_suggested  BIT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_assignments_article  FOREIGN KEY (article_id)  REFERENCES articles(id),
    CONSTRAINT fk_assignments_editor   FOREIGN KEY (editor_id)   REFERENCES users(id),
    CONSTRAINT fk_assignments_assigner FOREIGN KEY (assigned_by) REFERENCES users(id)
);
CREATE INDEX ix_assignments_editor ON assignments(editor_id);

/* BTV đổi thể loại áp dụng của bài trong lúc xử lý. */
CREATE TABLE category_change_logs (
    id                INT IDENTITY(1,1) PRIMARY KEY,
    assignment_id     INT        NOT NULL,
    from_category_id  INT        NOT NULL,
    to_category_id    INT        NOT NULL,
    changed_by        INT        NOT NULL,
    changed_at        DATETIME2  NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_category_change_logs_assignment FOREIGN KEY (assignment_id)    REFERENCES assignments(id),
    CONSTRAINT fk_category_change_logs_from       FOREIGN KEY (from_category_id) REFERENCES genres(id),
    CONSTRAINT fk_category_change_logs_to         FOREIGN KEY (to_category_id)   REFERENCES genres(id),
    CONSTRAINT fk_category_change_logs_changer    FOREIGN KEY (changed_by)       REFERENCES users(id),
    CONSTRAINT ck_category_change_logs_diff       CHECK (from_category_id <> to_category_id)
);

/* Bộ tiêu chí chấm theo thể loại, có phiên bản. Không sửa bộ đã ACTIVE,
   chỉ tạo bản mới. DRAFT -> ACTIVE -> RETIRED, mỗi thể loại 1 bộ ACTIVE. */
CREATE TABLE criteria_set_versions (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    category_id   INT          NOT NULL,
    version_no    INT          NOT NULL,
    status        VARCHAR(10)  NOT NULL DEFAULT 'DRAFT',
    created_by    INT          NOT NULL,
    created_at    DATETIME2    NOT NULL DEFAULT SYSDATETIME(),
    activated_by  INT          NULL,
    activated_at  DATETIME2    NULL,
    CONSTRAINT fk_criteria_set_versions_category  FOREIGN KEY (category_id)  REFERENCES genres(id),
    CONSTRAINT fk_criteria_set_versions_creator   FOREIGN KEY (created_by)   REFERENCES users(id),
    CONSTRAINT fk_criteria_set_versions_activator FOREIGN KEY (activated_by) REFERENCES users(id),
    CONSTRAINT uq_criteria_set_versions_no        UNIQUE (category_id, version_no),
    CONSTRAINT ck_criteria_set_versions_status    CHECK (status IN ('DRAFT','ACTIVE','RETIRED')),
    CONSTRAINT ck_criteria_set_versions_activated CHECK (status = 'DRAFT'
                                                         OR (activated_by IS NOT NULL AND activated_at IS NOT NULL))
);
CREATE UNIQUE INDEX uq_criteria_set_versions_active ON criteria_set_versions(category_id) WHERE status = 'ACTIVE';

/* Tiêu chí trong 1 bộ. weight là %, tổng weight của 1 bộ = 100: kiểm ở
   service trước khi kích hoạt bộ. */
CREATE TABLE criteria (
    id                       INT IDENTITY(1,1) PRIMARY KEY,
    criteria_set_version_id  INT           NOT NULL,
    name                     NVARCHAR(100) NOT NULL,
    weight                   DECIMAL(5,2)  NOT NULL,
    sort_order               INT           NOT NULL DEFAULT 1,
    CONSTRAINT fk_criteria_set     FOREIGN KEY (criteria_set_version_id) REFERENCES criteria_set_versions(id),
    CONSTRAINT ck_criteria_weight  CHECK (weight > 0 AND weight <= 100)
);
CREATE INDEX ix_criteria_set ON criteria(criteria_set_version_id);

/* Một lần chấm điểm bài trên 1 bản (version_id). Nộp xong (submitted_at) là
   khoá, không sửa. Trưởng ban trả lại thì tạo lần chấm mới với
   replaces_attempt_id trỏ lần cũ.
   attempt_type [ĐỀ XUẤT]: INITIAL (lần đầu) | RESCORE (Trưởng ban trả chấm
   lại) | APPEAL (chấm lại do khiếu nại, khi đó appeal_id khác NULL).
   decision [ĐỀ XUẤT]: ACCEPT (đạt) | REVISION (trả PV sửa) | REJECT (từ chối).
   total_score = tổng (score * weight / 100), thang 0-100.
   is_cross_department = 1 khi người chấm khác ban với bài.
   FK appeal_id được thêm sau khi tạo bảng appeals (2 bảng trỏ vòng nhau). */
CREATE TABLE scoring_attempts (
    id                       INT IDENTITY(1,1) PRIMARY KEY,
    article_id               INT             NOT NULL,
    version_id               INT             NOT NULL,
    assignment_id            INT             NOT NULL,
    appeal_id                INT             NULL,
    reviewer_id              INT             NOT NULL,
    attempt_no               INT             NOT NULL,
    attempt_type             VARCHAR(10)     NOT NULL DEFAULT 'INITIAL',
    criteria_set_version_id  INT             NOT NULL,
    category_id              INT             NOT NULL,   -- thể loại tại lúc chấm
    total_score              DECIMAL(5,2)    NULL,       -- NULL khi đang chấm
    decision                 VARCHAR(10)     NULL,
    decision_note            NVARCHAR(1000)  NULL,
    replaces_attempt_id      INT             NULL,
    is_cross_department      BIT             NOT NULL DEFAULT 0,
    opened_at                DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    submitted_at             DATETIME2       NULL,
    CONSTRAINT fk_scoring_attempts_article    FOREIGN KEY (article_id)              REFERENCES articles(id),
    CONSTRAINT fk_scoring_attempts_version    FOREIGN KEY (version_id)              REFERENCES article_versions(id),
    CONSTRAINT fk_scoring_attempts_assignment FOREIGN KEY (assignment_id)           REFERENCES assignments(id),
    CONSTRAINT fk_scoring_attempts_reviewer   FOREIGN KEY (reviewer_id)             REFERENCES users(id),
    CONSTRAINT fk_scoring_attempts_set        FOREIGN KEY (criteria_set_version_id) REFERENCES criteria_set_versions(id),
    CONSTRAINT fk_scoring_attempts_category   FOREIGN KEY (category_id)             REFERENCES genres(id),
    CONSTRAINT fk_scoring_attempts_replaces   FOREIGN KEY (replaces_attempt_id)     REFERENCES scoring_attempts(id),
    CONSTRAINT uq_scoring_attempts_no         UNIQUE (article_id, attempt_no),
    CONSTRAINT ck_scoring_attempts_type       CHECK (attempt_type IN ('INITIAL','RESCORE','APPEAL')),
    -- Chỉ lần chấm loại APPEAL mới có appeal_id.
    CONSTRAINT ck_scoring_attempts_appeal     CHECK ((attempt_type =  'APPEAL' AND appeal_id IS NOT NULL) OR
                                                     (attempt_type <> 'APPEAL' AND appeal_id IS NULL)),
    CONSTRAINT ck_scoring_attempts_decision   CHECK (decision IN ('ACCEPT','REVISION','REJECT')),
    CONSTRAINT ck_scoring_attempts_score      CHECK (total_score BETWEEN 0 AND 100),
    -- Đã nộp thì phải có điểm và quyết định.
    CONSTRAINT ck_scoring_attempts_submitted  CHECK (submitted_at IS NULL
                                                     OR (total_score IS NOT NULL AND decision IS NOT NULL))
);
CREATE INDEX ix_scoring_attempts_reviewer ON scoring_attempts(reviewer_id);

/* Điểm từng tiêu chí của 1 lần chấm, thang 0-100. */
CREATE TABLE scoring_attempt_details (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    scoring_attempt_id  INT             NOT NULL,
    criterion_id        INT             NOT NULL,
    score               DECIMAL(5,2)    NOT NULL,
    comment             NVARCHAR(1000)  NULL,
    CONSTRAINT fk_scoring_attempt_details_attempt   FOREIGN KEY (scoring_attempt_id) REFERENCES scoring_attempts(id),
    CONSTRAINT fk_scoring_attempt_details_criterion FOREIGN KEY (criterion_id)       REFERENCES criteria(id),
    CONSTRAINT uq_scoring_attempt_details           UNIQUE (scoring_attempt_id, criterion_id),
    CONSTRAINT ck_scoring_attempt_details_score     CHECK (score BETWEEN 0 AND 100)
);

/* Trưởng ban duyệt 1 lần chấm: APPROVED (chốt điểm) hoặc RETURNED (trả chấm
   lại, bắt buộc có lý do). announced_at = lúc công bố điểm cho PV, mở cửa
   sổ khiếu nại. */
CREATE TABLE score_approvals (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    scoring_attempt_id  INT             NOT NULL UNIQUE,
    decision            VARCHAR(10)     NOT NULL,
    reason              NVARCHAR(1000)  NULL,
    decided_by          INT             NOT NULL,
    decided_at          DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    announced_at        DATETIME2       NULL,
    CONSTRAINT fk_score_approvals_attempt FOREIGN KEY (scoring_attempt_id) REFERENCES scoring_attempts(id),
    CONSTRAINT fk_score_approvals_decider FOREIGN KEY (decided_by)         REFERENCES users(id),
    CONSTRAINT ck_score_approvals_decision CHECK (decision IN ('APPROVED','RETURNED')),
    CONSTRAINT ck_score_approvals_reason   CHECK (decision = 'APPROVED' OR reason IS NOT NULL),
    CONSTRAINT ck_score_approvals_announce CHECK (announced_at IS NULL OR decision = 'APPROVED')
);

/* ==========================================================================
   APPEAL - BF-03 Khiếu nại & kiểm soát điểm
   ========================================================================== */

/* PV khiếu nại điểm đã công bố, trong appeal_window_days ngày và tối đa
   max_appeals_per_article lần (appeal_rule_configs): kiểm ở service.
   SUBMITTED -> IN_REVIEW -> RESOLVED. */
CREATE TABLE appeals (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    article_id          INT          NOT NULL,
    scoring_attempt_id  INT          NOT NULL,   -- lần chấm bị khiếu nại
    appellant_id        INT          NOT NULL,
    rule_config_id      INT          NOT NULL,   -- đóng băng luật áp dụng tại lúc gửi (BR-QC-12)
    status              VARCHAR(10)  NOT NULL DEFAULT 'SUBMITTED',
    submitted_at        DATETIME2    NOT NULL DEFAULT SYSDATETIME(),
    resolved_at         DATETIME2    NULL,
    CONSTRAINT fk_appeals_article   FOREIGN KEY (article_id)         REFERENCES articles(id),
    CONSTRAINT fk_appeals_attempt   FOREIGN KEY (scoring_attempt_id) REFERENCES scoring_attempts(id),
    CONSTRAINT fk_appeals_appellant FOREIGN KEY (appellant_id)       REFERENCES users(id),
    CONSTRAINT uq_appeals_article   UNIQUE (article_id),
    CONSTRAINT ck_appeals_status    CHECK (status IN ('SUBMITTED','IN_REVIEW','RESOLVED')),
    CONSTRAINT ck_appeals_resolved  CHECK ((status =  'RESOLVED' AND resolved_at IS NOT NULL) OR
                                           (status <> 'RESOLVED' AND resolved_at IS NULL))
);
CREATE INDEX ix_appeals_article ON appeals(article_id);

ALTER TABLE scoring_attempts
    ADD CONSTRAINT fk_scoring_attempts_appeal FOREIGN KEY (appeal_id) REFERENCES appeals(id);

/* Tiêu chí bị khiếu nại, mỗi tiêu chí kèm lý do. */
CREATE TABLE appeal_criteria (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    appeal_id     INT             NOT NULL,
    criterion_id  INT             NOT NULL,
    reason        NVARCHAR(1000)  NOT NULL,
    CONSTRAINT fk_appeal_criteria_appeal    FOREIGN KEY (appeal_id)    REFERENCES appeals(id),
    CONSTRAINT fk_appeal_criteria_criterion FOREIGN KEY (criterion_id) REFERENCES criteria(id),
    CONSTRAINT uq_appeal_criteria           UNIQUE (appeal_id, criterion_id)
);

/* Kết quả cuối của khiếu nại. effective_attempt_id = lần chấm được dùng làm
   điểm chính thức sau khiếu nại (có thể là lần cũ hoặc lần chấm lại).
   rule_applied [ĐỀ XUẤT]: KEEP_ORIGINAL (lệch <= variance_threshold, giữ
   điểm cũ) | USE_RESCORE (lệch lớn, dùng điểm chấm lại). */
CREATE TABLE appeal_resolutions (
    id                    INT IDENTITY(1,1) PRIMARY KEY,
    appeal_id             INT          NOT NULL UNIQUE,
    rule_applied          VARCHAR(20)  NOT NULL,
    effective_attempt_id  INT          NOT NULL,
    effective_score       DECIMAL(5,2) NULL, -- Diem so da xac nhan; NULL cho du lieu cu dung effective_attempt_id.
    finalized_by          INT          NOT NULL,
    finalized_at          DATETIME2    NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_appeal_resolutions_appeal    FOREIGN KEY (appeal_id)            REFERENCES appeals(id),
    CONSTRAINT fk_appeal_resolutions_attempt   FOREIGN KEY (effective_attempt_id) REFERENCES scoring_attempts(id),
    CONSTRAINT fk_appeal_resolutions_finalizer FOREIGN KEY (finalized_by)         REFERENCES users(id),
    CONSTRAINT ck_appeal_resolutions_score CHECK (effective_score BETWEEN 0 AND 100),
    CONSTRAINT ck_appeal_resolutions_rule      CHECK (rule_applied IN ('KEEP_ORIGINAL','USE_RESCORE'))
);

/* Cấu hình luật khiếu nại, có hiệu lực theo ngày. Dùng dòng is_active = 1
   có effective_from lớn nhất mà <= hôm nay. */
CREATE TABLE appeal_rule_configs (
    id                       INT IDENTITY(1,1) PRIMARY KEY,
    appeal_window_days       INT           NOT NULL,
    max_appeals_per_article  INT           NOT NULL,
    variance_threshold       DECIMAL(5,2)  NOT NULL,   -- độ lệch điểm cho phép, thang 0-100
    effective_from           DATE          NOT NULL,
    is_active                BIT           NOT NULL DEFAULT 1,
    updated_by               INT           NOT NULL,
    CONSTRAINT fk_appeal_rule_configs_updater FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT uq_appeal_rule_configs_effective UNIQUE (effective_from),
    CONSTRAINT ck_appeal_rule_configs_values  CHECK (appeal_window_days > 0 AND max_appeals_per_article > 0
                                                     AND variance_threshold BETWEEN 0 AND 100)
);

ALTER TABLE appeals
    ADD CONSTRAINT fk_appeals_rule_config FOREIGN KEY (rule_config_id) REFERENCES appeal_rule_configs(id);

/* Đợt chấm chéo: lấy mẫu bài đã chấm trong khoảng ngày, giao người khác
   chấm lại để đo độ lệch của BTV. */
CREATE TABLE cross_review_batches (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    batch_name    NVARCHAR(100) NOT NULL,
    period_start  DATE          NOT NULL,
    period_end    DATE          NOT NULL,
    created_by    INT           NOT NULL,
    status        VARCHAR(10)   NOT NULL DEFAULT 'OPEN',
    created_at    DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_cross_review_batches_creator FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT ck_cross_review_batches_dates   CHECK (period_start <= period_end),
    CONSTRAINT ck_cross_review_batches_status  CHECK (status IN ('OPEN','CLOSED'))
);

/* deviation = score - điểm của original_scoring_id (có thể âm). */
CREATE TABLE cross_review_tasks (
    id                   INT IDENTITY(1,1) PRIMARY KEY,
    batch_id             INT           NOT NULL,
    article_id           INT           NOT NULL,
    reviewer_id          INT           NOT NULL,
    original_scoring_id  INT           NOT NULL,
    score                DECIMAL(5,2)  NULL,
    deviation            DECIMAL(5,2)  NULL,
    status               VARCHAR(10)   NOT NULL DEFAULT 'PENDING',
    submitted_at         DATETIME2     NULL,
    CONSTRAINT fk_cross_review_tasks_batch    FOREIGN KEY (batch_id)            REFERENCES cross_review_batches(id),
    CONSTRAINT fk_cross_review_tasks_article  FOREIGN KEY (article_id)          REFERENCES articles(id),
    CONSTRAINT fk_cross_review_tasks_reviewer FOREIGN KEY (reviewer_id)         REFERENCES users(id),
    CONSTRAINT fk_cross_review_tasks_original FOREIGN KEY (original_scoring_id) REFERENCES scoring_attempts(id),
    CONSTRAINT uq_cross_review_tasks          UNIQUE (batch_id, article_id),
    CONSTRAINT ck_cross_review_tasks_score    CHECK (score BETWEEN 0 AND 100),
    CONSTRAINT ck_cross_review_tasks_status   CHECK (status IN ('PENDING','DONE')),
    CONSTRAINT ck_cross_review_tasks_done     CHECK (status = 'PENDING'
                                                     OR (score IS NOT NULL AND deviation IS NOT NULL
                                                         AND submitted_at IS NOT NULL))
);

/* Chỉ số lệch của từng BTV theo tháng. period = ngày đầu tháng (vd 2026-09-01).
   overturn_rate = % khiếu nại bị lật điểm. Job tính lại, ghi đè dòng cũ. */
CREATE TABLE reviewer_deviation_indices (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    editor_id       INT           NOT NULL,
    period          DATE          NOT NULL,
    avg_deviation   DECIMAL(5,2)  NOT NULL DEFAULT 0,
    appeal_count    INT           NOT NULL DEFAULT 0,
    overturn_rate   DECIMAL(5,2)  NOT NULL DEFAULT 0,
    workload_count  INT           NOT NULL DEFAULT 0,
    last_updated    DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_reviewer_deviation_indices_editor FOREIGN KEY (editor_id) REFERENCES users(id),
    CONSTRAINT uq_reviewer_deviation_indices        UNIQUE (editor_id, period),
    CONSTRAINT ck_reviewer_deviation_indices_period CHECK (DAY(period) = 1),
    CONSTRAINT ck_reviewer_deviation_indices_values CHECK (appeal_count >= 0 AND workload_count >= 0
                                                           AND overturn_rate BETWEEN 0 AND 100)
);

/* ==========================================================================
   ROYALTY - BF-04 Chấm & chi nhuận bút
   ========================================================================== */

/* Giá trị 1 hệ số. amount = base_salary (lương cơ sở) x UNIT_PRICE_PERCENT / 100
   (system_parameters, hiện 10%), làm tròn xuống tới đồng.
   Đơn giá áp dụng = dòng có effective_from lớn nhất mà <= ngày đăng bài
   (BR-RY-03). Không sửa dòng cũ, chỉ thêm dòng mới. */
CREATE TABLE royalty_rates (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    base_salary     DECIMAL(18,0) NOT NULL,
    amount          DECIMAL(18,0) NOT NULL,
    effective_from  DATE          NOT NULL UNIQUE,
    created_by      INT           NOT NULL,   -- Kế toán / Admin
    CONSTRAINT fk_royalty_rates_creator FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT ck_royalty_rates_amount  CHECK (base_salary > 0 AND amount > 0)
);

/* Kỳ nhuận bút theo tháng (BR-RY-07):
   OPEN -> CLOSED (TKTS chốt) -> LOCKED (Kế toán khoá) -> PAID (đã chi hết).
   Kế toán trả kỳ CLOSED về OPEN kèm return_note (ghi returned_by).
   Từ LOCKED trở đi mọi số liệu của kỳ là BẤT BIẾN (BR-RY-06). */
CREATE TABLE royalty_periods (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    period_year   SMALLINT        NOT NULL,
    period_month  TINYINT         NOT NULL,
    status        VARCHAR(10)     NOT NULL DEFAULT 'OPEN',
    locked_by     INT             NULL,
    locked_at     DATETIME2       NULL,
    returned_by   INT             NULL,      -- lần trả về gần nhất
    return_note   NVARCHAR(1000)  NULL,
    paid_at       DATETIME2       NULL,
    CONSTRAINT fk_royalty_periods_locker   FOREIGN KEY (locked_by)   REFERENCES users(id),
    CONSTRAINT fk_royalty_periods_returner FOREIGN KEY (returned_by) REFERENCES users(id),
    CONSTRAINT uq_royalty_periods          UNIQUE (period_year, period_month),
    CONSTRAINT ck_royalty_periods_month    CHECK (period_month BETWEEN 1 AND 12),
    CONSTRAINT ck_royalty_periods_status   CHECK (status IN ('OPEN','CLOSED','LOCKED','PAID')),
    -- Qua mốc nào thì phải ghi ai làm và lúc nào.
    CONSTRAINT ck_royalty_periods_locked   CHECK (status IN ('OPEN','CLOSED')
                                                  OR (locked_by IS NOT NULL AND locked_at IS NOT NULL)),
    CONSTRAINT ck_royalty_periods_paid     CHECK ((status =  'PAID' AND paid_at IS NOT NULL) OR
                                                  (status <> 'PAID' AND paid_at IS NULL)),
    CONSTRAINT ck_royalty_periods_returned CHECK ((returned_by IS NULL AND return_note IS NULL)
                                                  OR (returned_by IS NOT NULL AND return_note IS NOT NULL))
);

/* Phiếu chấm nhuận bút phần bài viết. Mỗi lần tính lại là 1 PHIÊN BẢN MỚI
   (version_no tăng, bắt buộc recalc_reason), không sửa phiên bản cũ.
   Công thức (lưu đủ từng bước để giải trình), BR-RY-01..03:
     1. tier_rate        = score_used (nhóm bỏ chia bậc 90/70 ngày 13/10/2026,
                           giữ cột để không đổi cấu trúc, lưu bằng điểm)
     2. suggested_factor = base_factor * score_used / 100
     3. applied_factor   = suggested_factor (hệ thống tự tính, TKTS không nâng,
                           raise_reason luôn NULL)
     4. saturation_rate  = lấy từ articles (100 nếu không thuộc sự kiện hoặc
                           điểm >= events.exempt_score)
     5. nature_rate      = % theo tính chất (system_parameters NATURE_RATE_*):
                           ORIGINAL 100, TRANSLATION 70, COMPILATION 50, REPOST 30
     6. final_factor     = applied * saturation_rate/100 * nature_rate/100, tối thiểu 1
     7. amount           = final_factor * unit_price, làm tròn XUỐNG tới bội số
                           ROUNDING_UNIT (1.000đ)
   unit_price lấy từ rate_id (đơn giá hiệu lực tại ngày đăng).
   period_id = kỳ đang OPEN lúc hoàn tất chấm (BR-RY-05): kiểm ở service. */
CREATE TABLE royalty_gradings (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    scoring_attempt_id  INT            NOT NULL,   -- lần chấm có điểm đang dùng
    article_id          INT            NOT NULL,
    version_no          INT            NOT NULL DEFAULT 1,
    period_id           INT            NOT NULL,
    rate_id             INT            NOT NULL,
    category_id         INT            NOT NULL,
    score_used          DECIMAL(5,2)   NOT NULL,
    tier_rate           DECIMAL(5,2)   NOT NULL,
    base_factor         DECIMAL(5,2)   NOT NULL,
    suggested_factor    DECIMAL(5,2)   NOT NULL,
    applied_factor      DECIMAL(5,2)   NOT NULL,
    raise_reason        NVARCHAR(500)  NULL,
    saturation_rate     DECIMAL(5,2)   NOT NULL DEFAULT 100,
    nature              VARCHAR(20)    NOT NULL,
    nature_rate         DECIMAL(5,2)   NOT NULL,
    final_factor        DECIMAL(5,2)   NOT NULL,
    unit_price          DECIMAL(18,0)  NOT NULL,
    amount              DECIMAL(18,0)  NOT NULL,
    recalc_reason       NVARCHAR(500)  NULL,
    graded_by           INT            NOT NULL,   -- TKTS
    graded_at           DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_royalty_gradings_attempt  FOREIGN KEY (scoring_attempt_id) REFERENCES scoring_attempts(id),
    CONSTRAINT fk_royalty_gradings_article  FOREIGN KEY (article_id)         REFERENCES articles(id),
    CONSTRAINT fk_royalty_gradings_period   FOREIGN KEY (period_id)          REFERENCES royalty_periods(id),
    CONSTRAINT fk_royalty_gradings_rate     FOREIGN KEY (rate_id)            REFERENCES royalty_rates(id),
    CONSTRAINT fk_royalty_gradings_category FOREIGN KEY (category_id)        REFERENCES genres(id),
    CONSTRAINT fk_royalty_gradings_grader   FOREIGN KEY (graded_by)          REFERENCES users(id),
    CONSTRAINT uq_royalty_gradings_version  UNIQUE (article_id, version_no),
    CONSTRAINT ck_royalty_gradings_score    CHECK (score_used BETWEEN 0 AND 100),
    -- tier_rate = điểm nên được bằng 0 (bài 0 điểm vẫn có hệ số cuối tối thiểu 1).
    CONSTRAINT ck_royalty_gradings_rates    CHECK (tier_rate >= 0 AND tier_rate <= 100
                                                   AND saturation_rate > 0 AND saturation_rate <= 100
                                                   AND nature_rate > 0 AND nature_rate <= 100),
    CONSTRAINT ck_royalty_gradings_nature   CHECK (nature IN ('ORIGINAL','TRANSLATION','COMPILATION','REPOST')),
    -- Lưới an toàn từ thiết kế cũ (TKTS được nâng hệ số kèm lý do). Theo BR-RY-01
    -- mới, service luôn ghi applied = suggested và raise_reason = NULL.
    CONSTRAINT ck_royalty_gradings_applied  CHECK (applied_factor >= suggested_factor
                                                   AND (applied_factor = suggested_factor OR raise_reason IS NOT NULL)),
    CONSTRAINT ck_royalty_gradings_final    CHECK (final_factor >= 1),
    CONSTRAINT ck_royalty_gradings_amount   CHECK (unit_price > 0 AND amount >= 0),
    CONSTRAINT ck_royalty_gradings_recalc   CHECK (version_no = 1 OR recalc_reason IS NOT NULL)
);
CREATE INDEX ix_royalty_gradings_period ON royalty_gradings(period_id);

/* Hệ số từng ảnh/video của 1 phiếu chấm (BR-RY-04). Ảnh tái sử dụng (trùng
   với ảnh đã từng được trả nhuận bút, BR-IN-05) hoặc nguồn ngoài thì không
   tạo dòng ở đây. Attachment phải thuộc đúng bài của grading: kiểm ở service. */
CREATE TABLE royalty_media_items (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    grading_id     INT            NOT NULL,
    attachment_id  INT            NOT NULL,
    factor         DECIMAL(5,2)   NOT NULL,   -- = media_factors.factor
    unit_price     DECIMAL(18,0)  NOT NULL,
    amount         DECIMAL(18,0)  NOT NULL,   -- = factor * unit_price
    CONSTRAINT fk_royalty_media_items_grading    FOREIGN KEY (grading_id)    REFERENCES royalty_gradings(id),
    CONSTRAINT fk_royalty_media_items_attachment FOREIGN KEY (attachment_id) REFERENCES article_attachments(id),
    CONSTRAINT uq_royalty_media_items            UNIQUE (grading_id, attachment_id),
    CONSTRAINT ck_royalty_media_items_values     CHECK (factor > 0 AND unit_price > 0 AND amount >= 0)
);

/* Phiếu chi: mỗi người 1 phiếu mỗi kỳ, tạo khi khoá kỳ.
   gross_amount = tổng royalty_lines của người đó trong kỳ (có thể âm do
   điều chỉnh). debt_in = nợ mang từ kỳ trước.
   Nếu gross - debt_in >= 0: paid_amount = gross - debt_in, debt_out = 0.
   Ngược lại:                paid_amount = 0, debt_out = debt_in - gross
   (nợ chuyển sang kỳ sau, UC-53 A2). */
CREATE TABLE royalty_payments (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    period_id     INT            NOT NULL,
    user_id       INT            NOT NULL,
    gross_amount  DECIMAL(18,0)  NOT NULL,
    debt_in       DECIMAL(18,0)  NOT NULL DEFAULT 0,
    paid_amount   DECIMAL(18,0)  NOT NULL,
    debt_out      DECIMAL(18,0)  NOT NULL DEFAULT 0,
    status        VARCHAR(10)    NOT NULL DEFAULT 'UNPAID',
    paid_by       INT            NULL,      -- Kế toán
    paid_at       DATETIME2      NULL,
    CONSTRAINT fk_royalty_payments_period FOREIGN KEY (period_id) REFERENCES royalty_periods(id),
    CONSTRAINT fk_royalty_payments_user   FOREIGN KEY (user_id)   REFERENCES users(id),
    CONSTRAINT fk_royalty_payments_payer  FOREIGN KEY (paid_by)   REFERENCES users(id),
    CONSTRAINT uq_royalty_payments        UNIQUE (period_id, user_id),
    CONSTRAINT ck_royalty_payments_amount CHECK (debt_in >= 0 AND paid_amount >= 0 AND debt_out >= 0
                                                 AND paid_amount - debt_out = gross_amount - debt_in),
    CONSTRAINT ck_royalty_payments_status CHECK (
        (status = 'UNPAID' AND paid_by IS NULL     AND paid_at IS NULL) OR
        (status = 'PAID'   AND paid_by IS NOT NULL AND paid_at IS NOT NULL))
);

/* Dòng tiền của từng người trong kỳ.
   line_type:
     ARTICLE    = phần bài viết, chia theo share_percent, làm tròn 1.000đ,
                  phần dư cho tác giả chính (BR-RY-03)
     MEDIA      = phần ảnh/video, trả cho tác giả ảnh (media_item_id)
     ADJUSTMENT = chênh lệch khi tính lại bài thuộc kỳ đã LOCKED, ghi vào kỳ
                  sau; original_line_id trỏ dòng cũ, amount có thể âm (BR-RY-06)
   payment_id NULL cho tới lúc khoá kỳ thì gắn vào phiếu chi.
   Tổng các dòng ARTICLE của 1 grading phải bằng ĐÚNG grading.amount. */
CREATE TABLE royalty_lines (
    id                INT IDENTITY(1,1) PRIMARY KEY,
    period_id         INT            NOT NULL,
    grading_id        INT            NOT NULL,
    media_item_id     INT            NULL,
    original_line_id  INT            NULL,
    payment_id        INT            NULL,
    user_id           INT            NOT NULL,
    line_type         VARCHAR(10)    NOT NULL,
    share_percent     DECIMAL(5,2)   NULL,
    amount            DECIMAL(18,0)  NOT NULL,
    created_at        DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_royalty_lines_period   FOREIGN KEY (period_id)        REFERENCES royalty_periods(id),
    CONSTRAINT fk_royalty_lines_grading  FOREIGN KEY (grading_id)       REFERENCES royalty_gradings(id),
    CONSTRAINT fk_royalty_lines_media    FOREIGN KEY (media_item_id)    REFERENCES royalty_media_items(id),
    CONSTRAINT fk_royalty_lines_original FOREIGN KEY (original_line_id) REFERENCES royalty_lines(id),
    CONSTRAINT fk_royalty_lines_payment  FOREIGN KEY (payment_id)       REFERENCES royalty_payments(id),
    CONSTRAINT fk_royalty_lines_user     FOREIGN KEY (user_id)          REFERENCES users(id),
    CONSTRAINT ck_royalty_lines_type     CHECK (line_type IN ('ARTICLE','MEDIA','ADJUSTMENT')),
    CONSTRAINT ck_royalty_lines_shape    CHECK (
        (line_type = 'ARTICLE'    AND share_percent IS NOT NULL AND media_item_id IS NULL
                                  AND original_line_id IS NULL AND amount >= 0) OR
        (line_type = 'MEDIA'      AND media_item_id IS NOT NULL AND share_percent IS NULL
                                  AND original_line_id IS NULL AND amount >= 0) OR
        (line_type = 'ADJUSTMENT' AND original_line_id IS NOT NULL)),
    CONSTRAINT ck_royalty_lines_share    CHECK (share_percent > 0 AND share_percent <= 100)
);
CREATE INDEX ix_royalty_lines_period_user ON royalty_lines(period_id, user_id);
CREATE INDEX ix_royalty_lines_payment     ON royalty_lines(payment_id);
-- Chặn ghi trùng khi chạy lại: mỗi người 1 dòng tiền bài / grading,
-- mỗi ảnh/video đúng 1 dòng.
CREATE UNIQUE INDEX uq_royalty_lines_article ON royalty_lines(grading_id, user_id) WHERE line_type = 'ARTICLE';
CREATE UNIQUE INDEX uq_royalty_lines_media   ON royalty_lines(media_item_id)       WHERE line_type = 'MEDIA';

/* ==========================================================================
   KPI - BF-05 KPI & báo cáo
   ========================================================================== */

/* Chỉ tiêu tháng của từng PV do Trưởng ban đặt. min_avg_score thang 0-100. */
CREATE TABLE kpi_targets (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    reporter_id     INT          NOT NULL,
    target_year     SMALLINT     NOT NULL,
    target_month    TINYINT      NOT NULL,
    min_published   INT          NOT NULL,   -- số bài xuất bản tối thiểu
    min_avg_score   DECIMAL(5,2) NOT NULL,   -- điểm chất lượng TB tối thiểu
    set_by          INT          NOT NULL,   -- Trưởng ban
    CONSTRAINT fk_kpi_targets_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_kpi_targets_setter   FOREIGN KEY (set_by)      REFERENCES users(id),
    CONSTRAINT uq_kpi_targets          UNIQUE (reporter_id, target_year, target_month),
    CONSTRAINT ck_kpi_targets_month    CHECK (target_month BETWEEN 1 AND 12),
    CONSTRAINT ck_kpi_targets_min      CHECK (min_published >= 0 AND min_avg_score BETWEEN 0 AND 100)
);

/* Kết quả KPI tháng. DRAFT = số tạm, tính lại được; FINALIZED = đã chốt,
   không đổi nữa (dùng cho báo cáo). */
CREATE TABLE kpi_snapshots (
    id                INT IDENTITY(1,1) PRIMARY KEY,
    reporter_id       INT             NOT NULL,
    period_year       SMALLINT        NOT NULL,
    period_month      TINYINT         NOT NULL,
    actual_articles   INT             NOT NULL DEFAULT 0,
    actual_avg_score  DECIMAL(5,2)    NULL,       -- NULL khi chưa có bài nào có điểm
    status            VARCHAR(10)     NOT NULL DEFAULT 'DRAFT',
    notes             NVARCHAR(1000)  NULL,
    finalized_by      INT             NULL,
    finalized_at      DATETIME2       NULL,
    CONSTRAINT fk_kpi_snapshots_reporter  FOREIGN KEY (reporter_id)  REFERENCES users(id),
    CONSTRAINT fk_kpi_snapshots_finalizer FOREIGN KEY (finalized_by) REFERENCES users(id),
    CONSTRAINT uq_kpi_snapshots           UNIQUE (reporter_id, period_year, period_month),
    CONSTRAINT ck_kpi_snapshots_month     CHECK (period_month BETWEEN 1 AND 12),
    CONSTRAINT ck_kpi_snapshots_values    CHECK (actual_articles >= 0 AND actual_avg_score BETWEEN 0 AND 100),
    CONSTRAINT ck_kpi_snapshots_status    CHECK (
        (status = 'DRAFT'     AND finalized_by IS NULL     AND finalized_at IS NULL) OR
        (status = 'FINALIZED' AND finalized_by IS NOT NULL AND finalized_at IS NOT NULL))
);

/* Mỗi lần nhập file CSV số liệu độc giả. */
CREATE TABLE csv_import_logs (
    id               INT IDENTITY(1,1) PRIMARY KEY,
    file_name        NVARCHAR(255) NOT NULL,
    total_records    INT           NOT NULL,
    success_records  INT           NOT NULL,
    imported_by      INT           NOT NULL,
    imported_at      DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_csv_import_logs_importer FOREIGN KEY (imported_by) REFERENCES users(id),
    CONSTRAINT ck_csv_import_logs_records  CHECK (success_records >= 0 AND success_records <= total_records)
);

/* Số liệu độc giả theo ngày của 1 bài, nhập từ CSV.
   Điểm đánh giá TB = rating_sum / rating_count (sao 1-5). */
CREATE TABLE reader_interactions (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    article_id     INT        NOT NULL,
    record_date    DATE       NOT NULL,
    page_views     INT        NOT NULL DEFAULT 0,
    rating_count   INT        NOT NULL DEFAULT 0,
    rating_sum     INT        NOT NULL DEFAULT 0,
    import_log_id  INT        NOT NULL,
    created_at     DATETIME2  NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_reader_interactions_article FOREIGN KEY (article_id)    REFERENCES articles(id),
    CONSTRAINT fk_reader_interactions_import  FOREIGN KEY (import_log_id) REFERENCES csv_import_logs(id),
    CONSTRAINT uq_reader_interactions         UNIQUE (article_id, record_date),
    CONSTRAINT ck_reader_interactions_values  CHECK (page_views >= 0 AND rating_count >= 0
                                                     AND rating_sum BETWEEN rating_count AND rating_count * 5)
);
GO

PRINT N'Tạo schema thành công (40 bảng). Chạy tiếp data.sql để có dữ liệu mẫu.';
GO
