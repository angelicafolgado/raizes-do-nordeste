package br.com.raizesdonordeste.domain;

/** Regras de transição independentes de HTTP e persistência. */
public final class OrderRules {
    private OrderRules() {}
    public static void validateTransition(String previous, String next) {
        boolean valid = switch (previous) {
            case "AGUARDANDO_PAGAMENTO" -> next.equals("CANCELADO");
            case "EM_PREPARO" -> next.equals("PRONTO");
            case "PRONTO" -> next.equals("ENTREGUE");
            default -> false;
        };
        if (!valid) throw new ApiException("TRANSICAO_INVALIDA", "Transição de status não permitida.", 409);
    }
}
