module modules/veiculo

open modules/usuario

sig Placa, Modelo, Cor {}

abstract sig TipoVeiculo {}
one sig Carro, Moto extends TipoVeiculo {}

sig Veiculo {
    placa: one Placa,
    modelo: one Modelo,
    cor: lone Cor,
    tipo: one TipoVeiculo,
    dono: one Usuario
}

fact PlacaUnica {
    all disj v1, v2: Veiculo | v1.placa != v2.placa
}

pred podeGerenciarVeiculo[u: Usuario, v: Veiculo] {
    autenticado[u]
    v.dono = u
}

run { some u: Usuario, v: Veiculo | podeGerenciarVeiculo[u, v] } for 4
