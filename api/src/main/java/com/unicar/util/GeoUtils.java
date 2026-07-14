package com.unicar.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utilitário para cálculos geográficos compartilhados entre services
 * que lidam com localização (CaronaService, BuscaCaronaService).
 */
public final class GeoUtils {

    private static final double RAIO_TERRA_KM = 6371;

    private GeoUtils() {}

    /**
     * Calcula a distância em km entre dois pontos geográficos usando a fórmula de Haversine.
     */
    public static BigDecimal calcularDistanciaKm(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distancia = RAIO_TERRA_KM * c;

        return BigDecimal.valueOf(distancia).setScale(2, RoundingMode.HALF_UP);
    }
}