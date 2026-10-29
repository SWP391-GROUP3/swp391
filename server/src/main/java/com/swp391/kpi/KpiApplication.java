package com.swp391.kpi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lop khoi dong Spring Boot cua server (cong 8080). Chay: mvn -f server/pom.xml spring-boot:run.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   15  main()                          main
 */
@SpringBootApplication
public class KpiApplication {

    public static void main(String[] args) {
        SpringApplication.run(KpiApplication.class, args);
    }
}
