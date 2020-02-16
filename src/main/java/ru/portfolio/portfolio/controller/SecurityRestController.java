package ru.portfolio.portfolio.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.portfolio.portfolio.converter.SecurityEntityConverter;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.pojo.Security;
import ru.portfolio.portfolio.repository.SecurityRepository;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class SecurityRestController {
    private final SecurityRepository securityRepository;
    private final SecurityEntityConverter securityEntityConverter;

    @GetMapping("/securities")
    public List<SecurityEntity> getSecurities() {
        return securityRepository.findAll();
    }

    /**
     * Get the entity.
     * If entity not exists NOT_FOUND http status will be retuned.
     */
    @GetMapping("/securities/{isin}")
    public ResponseEntity<SecurityEntity> getIssuerByTicker(@PathVariable("isin") String isin) {
        return securityRepository.findByIsin(isin)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Create a new entity.
     * In create case  method returns CREATE http status, Location header and updated version of entity in body.
     * If entiry already exists CONFLICT http status and Location header was returned.
     * @param security new entity
     */
    @PostMapping("/securities")
    public ResponseEntity<SecurityEntity> postSecurity(@Valid @RequestBody Security security) throws URISyntaxException {
        Optional<SecurityEntity> result = securityRepository.findByIsin(security.getIsin());
        if (result.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .location(getLocation(security))
                    .build();
        } else {
            return createEntity(security);
        }
    }

    /**
     * Update or create a new entity.
     * In update case method returns OK http status and updated version of entity in body.
     * In create case  method returns CREATE http status, Location header and updated version of entity in body.
     * @param isin updating or creating entity
     * @param security new version of entity
     * @throws URISyntaxException
     */
    @PutMapping("/securities/{isin}")
    public ResponseEntity<SecurityEntity> putEntity(@PathVariable("isin") String isin, @Valid @RequestBody Security security) throws URISyntaxException {
        if (!security.getIsin().equals(isin)) {
            throw new BadRequestException("ISIN код бумаги задан не верно");
        }
        Optional<SecurityEntity> result = securityRepository.findByIsin(isin);
        if (result.isPresent()) {
            return ResponseEntity.ok(saveAndFlush(security));
        } else {
            return createEntity(security);
        }
    }

    private SecurityEntity saveAndFlush(Security security) {
        return securityRepository.saveAndFlush(securityEntityConverter.toEntity(security));
    }

    /**
     * @return response entity with http CREATE status, Location http header and body
     */
    private ResponseEntity<SecurityEntity> createEntity(Security security) throws URISyntaxException {
        return ResponseEntity
                .created(getLocation(security))
                .body(saveAndFlush(security));
    }

    private URI getLocation(Security security) throws URISyntaxException {
        return new URI("/securities/" + security.getIsin());
    }

    @DeleteMapping("/securities/{isin}")
    public void delete(@PathVariable("isin") String isin) {
        securityRepository.findByIsin(isin)
                .ifPresent(securityRepository::delete);
    }
}
