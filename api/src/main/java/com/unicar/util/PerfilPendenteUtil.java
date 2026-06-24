package com.unicar.util;

import com.unicar.domain.Usuario;

public final class PerfilPendenteUtil {

    private static final String EMAIL_PENDENTE_DOMINIO = "@pendente.unicar";
    private static final String CPF_PENDENTE_PREFIX = "999";
    private static final String MATRICULA_PENDENTE_PREFIX = "PENDENTE_";
    private static final String NOME_PENDENTE_PREFIX = "PENDENTE_";

    private static final int MAX_NOME = 150;
    private static final int MAX_EMAIL = 150;
    private static final int MAX_MATRICULA = 30;
    private static final int MAX_CPF = 11;

    private PerfilPendenteUtil() {}

    public static String nomePendente(String login) {
        return truncar(prefixoComHash(NOME_PENDENTE_PREFIX, login), MAX_NOME);
    }

    public static String emailPendente(String login) {
        String local = normalizarLogin(login);
        int maxLocal = MAX_EMAIL - EMAIL_PENDENTE_DOMINIO.length();
        if (local.length() > maxLocal) {
            local = local.substring(0, maxLocal);
        }
        if (local.isBlank()) {
            local = "usuario";
        }
        return local + EMAIL_PENDENTE_DOMINIO;
    }

    public static String cpfPendente(String login) {
        return prefixoComHash(CPF_PENDENTE_PREFIX, login, MAX_CPF);
    }

    public static String cpfParaCadastro(String login) {
        String normalizado = normalizarLogin(login);
        if (normalizado.matches("\\d{11}")) {
            return normalizado;
        }
        return cpfPendente(login);
    }

    public static String matriculaPendente(String login) {
        String normalizado = normalizarLogin(login);
        if (normalizado.matches("\\d{6,12}")) {
            return truncar(normalizado, MAX_MATRICULA);
        }
        return truncar(prefixoComHash(MATRICULA_PENDENTE_PREFIX, login), MAX_MATRICULA);
    }

    public static boolean isNomePendente(String nome) {
        return nome != null && nome.startsWith(NOME_PENDENTE_PREFIX);
    }

    public static boolean isEmailPendente(String email) {
        return email != null && email.endsWith(EMAIL_PENDENTE_DOMINIO);
    }

    public static boolean isCpfPendente(String cpf) {
        return cpf != null && cpf.startsWith(CPF_PENDENTE_PREFIX);
    }

    public static boolean isMatriculaPendente(String matricula) {
        return matricula != null && matricula.startsWith(MATRICULA_PENDENTE_PREFIX);
    }

    public static boolean perfilCompleto(Usuario usuario) {
        return temNomeCadastrado(usuario)
            && temEmailCadastrado(usuario)
            && temMatriculaCadastrada(usuario)
            && temCpfCadastrado(usuario);
    }

    public static boolean temNomeCadastrado(Usuario usuario) {
        return temTexto(usuario.getNome()) && !isNomePendente(usuario.getNome());
    }

    public static boolean temEmailCadastrado(Usuario usuario) {
        return temTexto(usuario.getEmail()) && !isEmailPendente(usuario.getEmail());
    }

    public static boolean temCpfCadastrado(Usuario usuario) {
        return temTexto(usuario.getCpf()) && !isCpfPendente(usuario.getCpf());
    }

    public static boolean temMatriculaCadastrada(Usuario usuario) {
        return temTexto(usuario.getMatricula()) && !isMatriculaPendente(usuario.getMatricula());
    }

    private static String prefixoComHash(String prefixo, String login) {
        int hash = Math.abs(normalizarLogin(login).hashCode());
        return String.format("%s%08d", prefixo, hash % 100_000_000);
    }

    private static String prefixoComHash(String prefixo, String login, int tamanhoMaximo) {
        String valor = prefixoComHash(prefixo, login);
        if (valor.length() > tamanhoMaximo) {
            return valor.substring(0, tamanhoMaximo);
        }
        return valor;
    }

    private static String normalizarLogin(String login) {
        return login == null ? "" : login.trim().toLowerCase();
    }

    private static String truncar(String valor, int tamanhoMaximo) {
        if (valor.length() <= tamanhoMaximo) {
            return valor;
        }
        return valor.substring(0, tamanhoMaximo);
    }

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
