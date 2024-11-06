/*
 * InvestBook
 * Copyright (C) 2024  Spacious Team <spacious-team@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.api;

import org.junit.jupiter.api.Test;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Performance test
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb;mode=mysql;non_keywords=value")
class AbstractEntityRepositoryServiceTest {

    static final AtomicInteger i = new AtomicInteger();
    @Autowired
    SecurityRestController service;

    @Test
    void insert() {
        test("insert()", service::insert, false);
        test("insert()", service::insert, false);
        test("insert()", service::insert, true);
        test("insert()", service::insert, true);
    }

    @Test
    void createIfAbsent() {
        test("createIfAbsent()", service::createIfAbsent, false);
        test("createIfAbsent()", service::createIfAbsent, false);
        test("createIfAbsent()", service::createIfAbsent, true);
        test("createIfAbsent()", service::createIfAbsent, true);
    }

    void test(String name, Consumer<Security> consumer, boolean isIdNull) {
        long t0 = System.nanoTime();
        for (int i = 0; i < 1_000; i++) {
            Security security = createSecurity(isIdNull);
            consumer.accept(security);
        }
        String isIdNullMsg = isIdNull ? "without predefined ID" : "with always same ID (insert error)";
        System.out.println("Total time " + name + " (" + isIdNullMsg + "):" + Duration.ofNanos(System.nanoTime() - t0));
    }

    Security createSecurity(boolean isIdNull) {
        return Security.builder()
                .id(isIdNull ? null : 1)
                .type(SecurityType.STOCK)
                .name(String.valueOf(i.getAndIncrement()))
                .build();
    }
}