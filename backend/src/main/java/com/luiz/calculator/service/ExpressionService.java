package com.luiz.calculator.service;

import com.luiz.calculator.model.Expression;
import com.luiz.calculator.repository.ExpressionRepository;
import com.luiz.calculator.controller.ExpressionEvaluator; // Importando seu motor raiz
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map; // Importante para as múltiplas variáveis

@Service
public class ExpressionService {

    @Autowired
    private ExpressionRepository repository;

    public Expression salvar(String textoDaConta, Map<String, Double> variaveis) {
        Expression entidade = new Expression();
        String contaLimpa = textoDaConta.trim();

        entidade.setExpression(contaLimpa);
        entidade.setCreatedAt(LocalDateTime.now());
        entidade.setLastExecutedAt(LocalDateTime.now());
        entidade.setCreatedBy("Luiz Felipe");

        entidade.setResult(calcularLogica(contaLimpa, variaveis));

        return repository.save(entidade);
    }

    public Expression editar(Long id, String novoTexto, Map<String, Double> variaveis) {
        Expression entidade = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expressao nao encontrada"));

        String contaLimpa = novoTexto.trim();
        entidade.setExpression(contaLimpa);
        entidade.setResult(calcularLogica(contaLimpa, variaveis));
        entidade.setLastExecutedAt(LocalDateTime.now());

        return repository.save(entidade);
    }

    public void excluir(Long id) {
        repository.deleteById(id);
    }

    private Double calcularLogica(String expressao, Map<String, Double> variaveis) {
        try {
            // Chamamos o seu Evaluator raiz que resolve parênteses e variáveis
            return ExpressionEvaluator.avaliar(expressao, variaveis);
        } catch (Exception e) {
            System.out.println("Erro ao calcular [" + expressao + "]: " + e.getMessage());
            return null;
        }
    }

    public Page<Expression> listar(String termo, String dataStr, String criador, Pageable pageable) {
        try {
            String busca = limparFiltro(termo);
            String criadorBusca = limparFiltro(criador);
            LocalDateTime inicio = null;
            LocalDateTime fim = null;

            if (dataStr != null && !dataStr.isEmpty() && !dataStr.equals("null") && !dataStr.equals("undefined")) {
                LocalDate data = LocalDate.parse(dataStr);
                inicio = data.atStartOfDay();
                fim = data.atTime(LocalTime.MAX);
            }

            if (busca == null && criadorBusca == null && inicio == null) {
                return repository.findAll(pageable);
            }

            return repository.findAll(criarFiltro(busca, criadorBusca, inicio, fim), pageable);

        } catch (Exception e) {
            System.err.println("Erro ao filtrar expressoes: " + e.getMessage());
            return Page.empty(pageable);
        }
    }

    private String limparFiltro(String valor) {
        if (valor == null || valor.trim().isEmpty() || valor.equals("undefined") || valor.equals("null")) {
            return null;
        }
        return valor.trim();
    }

    private Specification<Expression> criarFiltro(String busca, String criadorBusca, LocalDateTime inicio, LocalDateTime fim) {
        return (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();
            if (busca != null) {
                String texto = "%" + busca.toLowerCase() + "%";
                Predicate porExpressao = cb.like(cb.lower(root.get("expression")), texto);
                Predicate porCriador = cb.like(cb.lower(root.get("createdBy")), texto);
                filtros.add(cb.or(porExpressao, porCriador));
            }
            if (criadorBusca != null) {
                filtros.add(cb.like(cb.lower(root.get("createdBy")), "%" + criadorBusca.toLowerCase() + "%"));
            }
            if (inicio != null && fim != null) {
                filtros.add(cb.between(root.get("createdAt"), inicio, fim));
            }
            return cb.and(filtros.toArray(new Predicate[0]));
        };
    }
}