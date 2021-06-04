/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.spacious_team.broker.pojo.SecurityDescription;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.investbook.converter.SecurityDescriptionConverter;
import ru.investbook.entity.SecurityDescriptionEntity;
import ru.investbook.repository.SecurityDescriptionRepository;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@Tag(name = "Информация по инструментам", description = "Сектор экономики, эмитент")
@RequestMapping("/api/v1/security-descriptions")
public class SecurityDescriptionRestController extends AbstractRestController<String, SecurityDescription, SecurityDescriptionEntity> {
    private final SecurityDescriptionRepository repository;

    public SecurityDescriptionRestController(SecurityDescriptionRepository repository, SecurityDescriptionConverter converter) {
        super(repository, converter);
        this.repository = repository;
    }

    @Override
    @GetMapping
    @Operation(summary = "Отобразить все", description = "Отобразить информацию по всем инструментам")
    public List<SecurityDescription> get() {
        return super.get();
    }


    @Override
    @GetMapping("{id}")
    @Operation(summary = "Отобразить один",
            description = "Отобразить информацию по инструменту")
    public ResponseEntity<SecurityDescription> get(@PathVariable("id")
                                        @Parameter(description = "Идентификатор", example = "ISIN, BR-2.21, USDRUB_TOM")
                                                String id) {
        return super.get(id);
    }


    @Override
    @PostMapping
    @Operation(summary = "Добавить", description = "Добавить информацию об акции, облигации, деривативе или валютной паре")
    public ResponseEntity<Void> post(@Valid @RequestBody SecurityDescription security) {
        return super.post(security);
    }

    @Override
    @PutMapping("{id}")
    @Operation(summary = "Обновить", description = "Добавить информацию об акции, облигации, деривативе или валютной паре")
    public ResponseEntity<Void> put(@PathVariable("id")
                                    @Parameter(description = "Идентификатор", example = "ISIN, BR-2.21, USDRUB_TOM")
                                            String id,
                                    @Valid @RequestBody SecurityDescription security) {
        return super.put(id, security);
    }

    @Override
    @DeleteMapping("{id}")
    @Operation(summary = "Удалить", description = "Удалить информацию по инструменту")
    public void delete(@PathVariable("id")
                       @Parameter(description = "Идентификатор", example = "ISIN, BR-2.21, USDRUB_TOM")
                               String id) {
        super.delete(id);
    }

    @Override
    protected Optional<SecurityDescriptionEntity> getById(String isin) {
        return repository.findById(isin);
    }

    @Override
    protected String getId(SecurityDescription object) {
        return object.getSecurity();
    }

    @Override
    protected SecurityDescription updateId(String id, SecurityDescription object) {
        return object.toBuilder().security(id).build();
    }

    @Override
    protected String getLocation() {
        return "/securities";
    }
}
