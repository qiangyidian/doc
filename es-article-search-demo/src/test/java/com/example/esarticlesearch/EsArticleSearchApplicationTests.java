package com.example.esarticlesearch;

import com.example.esarticlesearch.EsArticleSearchApplication;
import com.example.esarticlesearch.repository.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@SpringBootTest(classes = EsArticleSearchApplication.class)
class EsArticleSearchApplicationTests {

    @MockBean
    private ArticleRepository articleRepository;

    @MockBean
    private ElasticsearchOperations elasticsearchOperations;

    @Test
    void contextLoads(){

    }
}
