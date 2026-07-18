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

fact IntegridadeVeiculo {
    all disj v1, v2: Veiculo | v1.placa != v2.placa
}

fun veiculosDe[u: Usuario]: set Veiculo {
    dono.u
}

fun veiculosDeTipo[t: TipoVeiculo]: set Veiculo {
    { v: Veiculo | v.tipo = t }
}

pred podeCadastrarVeiculo[u: Usuario, p: Placa] {
    u.ativo = True
    no v: Veiculo | v.placa = p
}

pred podeAlterarVeiculo[u: Usuario, v: Veiculo] {
    v.dono = u
}

assert PlacaUnica {
    all disj v1, v2: Veiculo | v1.placa != v2.placa
}

assert ApenasDonoAlteraVeiculo {
    all u: Usuario, v: Veiculo | podeAlterarVeiculo[u, v] implies v.dono = u
}

assert VeiculoNaoPossuiDoisDonos {
    all disj u1, u2: Usuario | no veiculosDe[u1] & veiculosDe[u2]
}

check PlacaUnica for 5
check ApenasDonoAlteraVeiculo for 5
check VeiculoNaoPossuiDoisDonos for 5
run { some Veiculo } for 4
