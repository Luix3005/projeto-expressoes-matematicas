package com.luiz.calculator.model;

import java.util.Map;

public class CalculoRequest {
    private String expressao;
    private Map<String, Double> variaveis;

    // Getters e Setters (essencial para o Java conseguir ler o JSON)
    public String getExpressao() { return expressao; }
    public void setExpressao(String expressao) { this.expressao = expressao; }
    public Map<String, Double> getVariaveis() { return variaveis; }
    public void setVariaveis(Map<String, Double> variaveis) { this.variaveis = variaveis; }
}