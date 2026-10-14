-- noinspection SqlNoDataSourceInspectionForFile
-- Dev-only BF-03 fixtures. Run with sqlcmd -f 65001 to preserve Vietnamese text.
-- Inserts three independent articles for reporter an.nv; never resets existing data.
-- Existing marker titles are skipped, including articles already appealed or expired.
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM users u JOIN roles r ON r.id=u.role_id
                   WHERE u.id=1 AND u.username='an.nv' AND u.section_id=1
                     AND u.status='ACTIVE' AND r.code='REPORTER')
        THROW 51300, 'Expected active reporter an.nv (id=1, section=1) was not found.', 1;
    IF NOT EXISTS (SELECT 1 FROM users u JOIN roles r ON r.id=u.role_id
                   WHERE u.id=3 AND u.section_id=1 AND u.status='ACTIVE' AND r.code='EDITOR')
        THROW 51301, 'Expected active editor (id=3, section=1) was not found.', 1;
    IF NOT EXISTS (SELECT 1 FROM users u JOIN roles r ON r.id=u.role_id
                   WHERE u.id=5 AND u.section_id=1 AND u.status='ACTIVE' AND r.code='HEAD_OF_DEPARTMENT')
        THROW 51302, 'Expected active head (id=5, section=1) was not found.', 1;
    IF NOT EXISTS (SELECT 1 FROM criteria_set_versions WHERE id=1 AND category_id=3 AND status='ACTIVE')
       OR (SELECT COALESCE(SUM(weight),0) FROM criteria WHERE criteria_set_version_id=1) <> 100
        THROW 51303, 'The expected active criteria set with total weight 100 was not found.', 1;
    IF NOT EXISTS (SELECT 1 FROM appeal_rule_configs
                   WHERE is_active=1 AND effective_from<=CAST(SYSDATETIME() AS DATE))
        THROW 51304, 'No effective appeal rule configuration was found.', 1;

    DECLARE @samples TABLE(sequence_no INT PRIMARY KEY, title NVARCHAR(300),
                           content NVARCHAR(MAX), base_score DECIMAL(5,2));
    INSERT INTO @samples VALUES
    (1, N'[BF03-DEMO-01] Người trẻ chung tay làm sạch kênh sau mưa lớn',
        N'Sáng cuối tuần, các nhóm tình nguyện phối hợp với người dân thu gom rác tại tuyến kênh trong khu dân cư.
Bài viết ghi nhận hoạt động tại hiện trường, phỏng vấn người dân và đại diện nhóm tình nguyện.
Tác giả đề nghị xem xét lại tiêu chí tính phát hiện và độ khó tác nghiệp vì việc thu thập thông tin diễn ra trong điều kiện mưa lớn.', 60),
    (2, N'[BF03-DEMO-02] Chợ dân sinh ứng dụng thanh toán không tiền mặt',
        N'Nhiều tiểu thương tại chợ dân sinh bắt đầu sử dụng mã QR để phục vụ người mua.
Phóng viên khảo sát các quầy hàng, đối chiếu phản hồi của người mua với ý kiến ban quản lý chợ.
Tác giả có thể khiếu nại tiêu chí chất lượng trình bày vì bài đã bổ sung số liệu khảo sát và dẫn chứng cụ thể.', 65),
    (3, N'[BF03-DEMO-03] Lớp học miễn phí giúp người cao tuổi sử dụng dịch vụ công',
        N'Một lớp học tại nhà văn hóa hướng dẫn người cao tuổi tra cứu và nộp hồ sơ dịch vụ công trực tuyến.
Bài viết có phỏng vấn học viên, người hướng dẫn và ghi nhận các khó khăn trong quá trình thực hành.
Tác giả có thể đề nghị xem xét tiêu chí tính thời sự và giá trị thông tin của bài.', 70);

    DECLARE @sequence INT=1, @article_id INT, @version_id INT, @assignment_id INT, @attempt_id INT;
    DECLARE @title NVARCHAR(300), @content NVARCHAR(MAX), @base_score DECIMAL(5,2);
    DECLARE @announced_at DATETIME2, @total_score DECIMAL(5,2), @created_count INT=0;

    WHILE @sequence<=3
    BEGIN
        SELECT @title=title, @content=content, @base_score=base_score
        FROM @samples WHERE sequence_no=@sequence;
        IF NOT EXISTS (SELECT 1 FROM articles WITH (UPDLOCK, HOLDLOCK) WHERE title=@title)
        BEGIN
            SET @announced_at=DATEADD(MINUTE,-15*@sequence,SYSDATETIME());
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
                     WHEN 1 THEN N'Bài có góc tiếp cận riêng; cần làm rõ hơn phát hiện mới.'
                     WHEN 2 THEN N'Có ghi nhận hiện trường; cần bổ sung thông tin về điều kiện tác nghiệp.'
                     WHEN 3 THEN N'Bố cục rõ ràng, dẫn chứng phù hợp; có thể làm gọn một số đoạn.'
                     ELSE N'Thông tin có giá trị thời sự; cần nêu rõ thời điểm khảo sát.'
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
            SET @created_count=@created_count+1;
        END;
        SET @sequence=@sequence+1;
    END;
    COMMIT TRANSACTION;
    SELECT @created_count AS newly_created_articles;
    SELECT a.id,a.title,s.total_score,p.announced_at,
           DATEADD(DAY,cfg.appeal_window_days,p.announced_at) AS appeal_deadline,
           CASE WHEN EXISTS(SELECT 1 FROM appeals x WHERE x.article_id=a.id) THEN 1 ELSE 0 END AS already_appealed
    FROM articles a JOIN scoring_attempts s ON s.article_id=a.id
    JOIN score_approvals p ON p.scoring_attempt_id=s.id
    CROSS APPLY(SELECT TOP(1) appeal_window_days FROM appeal_rule_configs
                WHERE is_active=1 AND effective_from<=CAST(SYSDATETIME() AS DATE)
                ORDER BY effective_from DESC) cfg
    WHERE a.title IN(SELECT title FROM @samples)
    ORDER BY a.id;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
