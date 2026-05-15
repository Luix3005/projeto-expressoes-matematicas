package com.luiz.calculator.service;

import java.util.Map;

public class ExpressionEvaluator {

    public static double avaliar(String expressao, Map<String, Double> variaveis) {
        
        String contaProcessada = expressao.toLowerCase().replaceAll("\\s+", "");
        
        if (variaveis != null) {
            for (Map.Entry<String, Double> var : variaveis.entrySet()) {

                contaProcessada = contaProcessada.replace(var.getKey().toLowerCase(), String.valueOf(var.getValue()));
            }
        }

        return resolver(contaProcessada);
    }

    private static double resolver(String exp) {
        while (exp.contains("(")) {
            int fechaParen = exp.indexOf(")");
            int abreParen = exp.lastIndexOf("(", fechaParen);
            
            String subExpressao = exp.substring(abreParen + 1, fechaParen);
            double resultadoSub = calcularSimples(subExpressao);
            exp = exp.substring(0, abreParen) + resultadoSub + exp.substring(fechaParen + 1);
        }
        return calcularSimples(exp);
    }

    private static double calcularSimples(String exp) {
       
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < exp.length()) ? exp.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseSomaSub();
                return x;
            }

            double parseSomaSub() {
                double x = parseMultDiv();
                for (;;) {
                    if      (eat('+')) x += parseMultDiv();
                    else if (eat('-')) x -= parseMultDiv();
                    else return x;
                }
            }

            double parseMultDiv() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else if (eat('÷')) x /= parseFactor(); // Suporte ao símbolo que você usou
                    else return x;
                }
            }

            double parseFactor() {
                int startPos = this.pos;
                if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    return Double.parseDouble(exp.substring(startPos, this.pos));
                }
                return 0;
            }
        }.parse();
    }
}