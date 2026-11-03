package com.swp391.kpi.article.service;

import com.swp391.kpi.article.dto.ArticleReadResponse;
import com.swp391.kpi.article.repository.ArticleReadRepository;
import com.swp391.kpi.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ham doc bai cho luong khac (BF-03): kiem tra 1 nguoi co phai tac gia cua bai khong, lay thong tin
 * bai.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  field                           repository / service duoc inject
 *   27  isAuthor()                      kiem tra author
 *   30  getVersion()                    lay version
 *   37  getPublishedByAuthor()          lay published theo author
 *   42  getArticle()                    lay article
 */
@Service
@RequiredArgsConstructor
public class ArticleReadService {
    private final ArticleReadRepository repository;

    @Transactional(readOnly = true)
    public boolean isAuthor(Integer articleId, Integer userId) { return repository.isAuthor(articleId, userId); }

    @Transactional(readOnly = true)
    public com.swp391.kpi.article.dto.ArticleVersionReadResponse getVersion(Integer articleId, Integer versionId) {
        var versions = repository.findVersion(articleId, versionId);
        if (versions.isEmpty()) throw new NotFoundException("Không tìm thấy phiên bản bài được chấm.");
        return versions.get(0);
    }

    @Transactional(readOnly = true)
    public java.util.List<ArticleReadResponse> getPublishedByAuthor(Integer authorId) {
        return repository.findPublishedByAuthorId(authorId);
    }

    @Transactional(readOnly = true)
    public ArticleReadResponse getArticle(Integer id) {
        var articles = repository.findById(id);
        if (articles.isEmpty()) throw new NotFoundException("Không tìm thấy bài viết.");
        return articles.get(0);
    }
}
