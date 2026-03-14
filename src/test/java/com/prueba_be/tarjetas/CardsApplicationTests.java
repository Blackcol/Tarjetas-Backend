package com.prueba_be.tarjetas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import com.prueba_be.tarjetas.infrastructure.adapter.out.persistence.CardSpringDataRepository;
import com.prueba_be.tarjetas.infrastructure.adapter.out.persistence.TransactionSpringDataRepository;

@SpringBootTest
@ActiveProfiles("test")
class CardsApplicationTests {

    @MockitoBean
    CardSpringDataRepository cardSpringDataRepository;

    @MockitoBean
    TransactionSpringDataRepository transactionSpringDataRepository;

    @Test
    void contextLoads() {
    }

}
