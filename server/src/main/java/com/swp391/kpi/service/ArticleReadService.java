package com.swp391.kpi.service;

import com.swp391.kpi.dto.ArticleReadResponse;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.ArticleReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleReadService {
    private final ArticleReadRepository repository;

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
