-- noinspection SqlNoDataSourceInspectionForFile
-- Dev-only BF-03 fixture. Run with sqlcmd -f 65001 to preserve Vietnamese text.
-- Inserts ONE scored article (SCORE_PUBLISHED) for reporter an.nv, ready to be appealed.
-- Same shape as BF03_sample_appealable_articles.sql; skipped if the marker title already exists.
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM users u JOIN roles r ON r.id=u.role_id
                   WHERE u.id=1 AND u.username='an.nv' AND u.section_id=1
                     AND u.status='ACTIVE' AND r.code='REPORTER')
        THROW 51310, 'Expected active reporter an.nv (id=1, section=1) was not found.', 1;
    IF NOT EXISTS (SELECT 1 FROM users u JOIN roles r ON r.id=u.role_id
                   WHERE u.id=3 AND u.section_id=1 AND u.status='ACTIVE' AND r.code='EDITOR')
        THROW 51311, 'Expected active editor (id=3, section=1) was not found.', 1;
    IF NOT EXISTS (SELECT 1 FROM users u JOIN roles r ON r.id=u.role_id
                   WHERE u.id=5 AND u.section_id=1 AND u.status='ACTIVE' AND r.code='HEAD_OF_DEPARTMENT')
        THROW 51312, 'Expected active head (id=5, section=1) was not found.', 1;
    IF NOT EXISTS (SELECT 1 FROM criteria_set_versions WHERE id=1 AND category_id=3 AND status='ACTIVE')
       OR (SELECT COALESCE(SUM(weight),0) FROM criteria WHERE criteria_set_version_id=1) <> 100
        THROW 51313, 'The expected active criteria set with total weight 100 was not found.', 1;
    IF NOT EXISTS (SELECT 1 FROM appeal_rule_configs
                   WHERE is_active=1 AND effective_from<=CAST(SYSDATETIME() AS DATE))
        THROW 51314, 'No effective appeal rule configuration was found.', 1;

    DECLARE @title NVARCHAR(300)=N'[BF03-DEMO-04] Hà Nội thí điểm làn xe buýt riêng trên tuyến Nguyễn Trãi – Quang Trung';
    DECLARE @content NVARCHAR(MAX)=N'Từ đầu quý III, Sở Giao thông Vận tải Hà Nội đưa vào thí điểm làn xe buýt riêng dài khoảng 6 km trên trục Nguyễn Trãi – Quang Trung. Mục tiêu là cải thiện tốc độ và độ đúng giờ của vận tải công cộng ở khu vực thường xuyên ùn tắc.

Theo số liệu của đơn vị vận hành, thời gian một chuyến xe buýt đi hết tuyến giảm từ 48 phút xuống còn khoảng 39 phút vào giờ cao điểm. Lượng hành khách bình quân mỗi ngày tăng 12% so với cùng kỳ năm trước.

Anh Nguyễn Văn Minh (34 tuổi, nhân viên văn phòng ở Thanh Xuân) cho biết: "Trước đây tôi mất gần một tiếng để đi làm. Giờ xe buýt chạy thông hơn nên tôi chuyển hẳn từ xe máy sang xe buýt."

Tuy vậy, chương trình vẫn còn bất cập. Qua quan sát của phóng viên trong một buổi sáng, có hơn 40 lượt xe máy và ô tô cá nhân đi vào làn riêng. Lực lượng chức năng cho biết đã lập biên bản hơn 300 trường hợp vi phạm trong tháng đầu, nhưng thiếu camera giám sát nên khó xử lý triệt để.

Chuyên gia giao thông, TS. Lê Hoàng Nam, nhận định: "Làn riêng chỉ hiệu quả khi được thực thi nghiêm và có hạ tầng hỗ trợ. Cần lắp camera phạt nguội và mở rộng thêm điểm dừng thuận tiện."

Sở Giao thông Vận tải dự kiến đánh giá toàn diện vào cuối năm. Nếu kết quả khả quan, mô hình sẽ được nhân rộng sang các tuyến Giải Phóng và Xã Đàn.';
    DECLARE @base_score DECIMAL(5,2)=55;

    IF EXISTS (SELECT 1 FROM articles WITH (UPDLOCK, HOLDLOCK) WHERE title=@title)
    BEGIN
        COMMIT TRANSACTION;
        SELECT 0 AS newly_created_articles;
        RETURN;
    END;

    DECLARE @article_id INT, @version_id INT, @assignment_id INT, @attempt_id INT, @total_score DECIMAL(5,2);
    DECLARE @announced_at DATETIME2=DATEADD(MINUTE,-5,SYSDATETIME());

    INSERT INTO articles(author_id,section_id,declared_category_id,category_id,title,content,
                         status,submitted_at,created_at,updated_at)
    VALUES(1,1,3,3,@title,@content,'SCORE_PUBLISHED',
           DATEADD(HOUR,-3,@announced_at),DATEADD(HOUR,-4,@announced_at),@announced_at);
    SET @article_id=CAST(SCOPE_IDENTITY() AS INT);
    INSERT INTO article_authors(article_id,user_id,share_percent,confirm_status,responded_at)
    VALUES(@article_id,1,100,'ACCEPTED',DATEADD(HOUR,-4,@announced_at));
    INSERT INTO article_versions(article_id,version_no,kind,title,content,created_by,created_at)
    VALUES(@article_id,1,'SUBMISSION',@title,@content,1,DATEADD(HOUR,-3,@announced_at));
    SET @version_id=CAST(SCOPE_IDENTITY() AS INT);
    INSERT INTO assignments(article_id,editor_id,assigned_by,assigned_at)
    VALUES(@article_id,3,5,DATEADD(HOUR,-2,@announced_at));
    SET @assignment_id=CAST(SCOPE_IDENTITY() AS INT);
    INSERT INTO scoring_attempts(article_id,version_id,assignment_id,reviewer_id,attempt_no,
                                 attempt_type,criteria_set_version_id,category_id,total_score,
                                 decision,opened_at,submitted_at)
    VALUES(@article_id,@version_id,@assignment_id,3,1,'INITIAL',1,3,NULL,NULL,
           DATEADD(HOUR,-2,@announced_at),NULL);
    SET @attempt_id=CAST(SCOPE_IDENTITY() AS INT);
    INSERT INTO scoring_attempt_details(scoring_attempt_id,criterion_id,score,comment)
    SELECT @attempt_id,id,@base_score+5*sort_order,
           CASE sort_order
             WHEN 1 THEN N'Chủ đề có tính thời sự nhưng chưa khai thác sâu góc nhìn riêng.'
             WHEN 2 THEN N'Có số liệu và trích dẫn nhưng chưa kiểm chứng độc lập.'
             WHEN 3 THEN N'Bố cục rõ ràng, độ dài vừa phải.'
             ELSE N'Thông tin hữu ích cho bạn đọc; cần thêm nguồn số liệu cụ thể.'
           END
    FROM criteria WHERE criteria_set_version_id=1;
    SELECT @total_score=CAST(ROUND(SUM(d.score*c.weight/100),2) AS DECIMAL(5,2))
    FROM scoring_attempt_details d JOIN criteria c ON c.id=d.criterion_id
    WHERE d.scoring_attempt_id=@attempt_id;
    UPDATE scoring_attempts SET total_score=@total_score,decision='ACCEPT',
                                submitted_at=DATEADD(HOUR,-1,@announced_at)
    WHERE id=@attempt_id;
    INSERT INTO score_approvals(scoring_attempt_id,decision,decided_by,decided_at,announced_at)
    VALUES(@attempt_id,'APPROVED',5,DATEADD(MINUTE,-5,@announced_at),@announced_at);
    INSERT INTO article_status_logs(article_id,actor_id,action,from_status,to_status,note,created_at)
    VALUES
    (@article_id,1,'CREATE',NULL,'DRAFT',N'Bài mẫu BF-03 để thử gửi khiếu nại.',DATEADD(HOUR,-4,@announced_at)),
    (@article_id,1,'SUBMIT','DRAFT','SUBMITTED',NULL,DATEADD(HOUR,-3,@announced_at)),
    (@article_id,5,'ASSIGN','SUBMITTED','ASSIGNED',NULL,DATEADD(HOUR,-2,@announced_at)),
    (@article_id,3,'SUBMIT_SCORE','ASSIGNED','PENDING_APPROVAL',NULL,DATEADD(HOUR,-1,@announced_at)),
    (@article_id,5,'APPROVE_SCORE','PENDING_APPROVAL','SCORE_PUBLISHED',NULL,@announced_at);
    INSERT INTO notifications(user_id,article_id,message,created_at)
    VALUES(1,@article_id,N'Bài mẫu đã được công bố điểm. Bạn có thể gửi khiếu nại trong thời hạn cấu hình.',@announced_at);
    INSERT INTO audit_logs(actor_id,action_type,entity_type,entity_id,old_value,new_value)
    VALUES(NULL,'CREATE','articles',@article_id,NULL,
           (SELECT N'BF03 sample seed' AS source,@title AS title,@attempt_id AS scoringAttemptId,
                   @total_score AS totalScore,'SCORE_PUBLISHED' AS status FOR JSON PATH,WITHOUT_ARRAY_WRAPPER));
    COMMIT TRANSACTION;
    SELECT 1 AS newly_created_articles, @article_id AS article_id, @total_score AS total_score;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
