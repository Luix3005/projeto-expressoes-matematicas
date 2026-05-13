package com.luiz.calculator.controller;

import com.luiz.calculator.model.Expression;
import com.luiz.calculator.service.ExpressionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calculator")
@CrossOrigin(origins = "*")
public class ExpressionController {

    @Autowired
    private ExpressionService service;

    @PostMapping
    public Expression salvar(
        @RequestParam String textoDaConta,
        @RequestParam(required = false) Double valorX
    ) {
        return service.salvar(textoDaConta, valorX);
    }

    
    @GetMapping("/todos")
    public ResponseEntity<Page<Expression>> listar(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String termo,
        @RequestParam(required = false) String dataStr,
        @RequestParam(required = false) String criador
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Expression> resultado = service.listar(termo, dataStr, criador, pageable);
        return ResponseEntity.ok(resultado);
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }

    @PutMapping("/{id}")
    public Expression editar(
        @PathVariable Long id, 
        @RequestParam String novoTexto, 
        @RequestParam(required = false) Double valorX
    ) {
        return service.editar(id, novoTexto, valorX);
    }
}