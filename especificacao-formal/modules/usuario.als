module modules/usuario

sig Matricula, CPF, Email, SessaoToken {}

abstract sig Genero {}
one sig Masculino, Feminino, OutroGenero, NaoInformado extends Genero {}

abstract sig Bool {}
one sig True, False extends Bool {}

sig Usuario {
    matricula: one Matricula,
    cpf: one CPF,
    email: one Email,
    genero: one Genero,
    ativo: one Bool,
    receberEmail: one Bool,
    sessoes: set SessaoToken
}

sig TokenRevogado {
    token: one SessaoToken
}

sig BloqueioUsuario {
    bloqueador: one Usuario,
    bloqueado: one Usuario
}

fact IntegridadeUsuario {
    all disj u1, u2: Usuario |
        u1.matricula != u2.matricula and u1.cpf != u2.cpf and u1.email != u2.email
    all b: BloqueioUsuario | b.bloqueador != b.bloqueado
    all disj b1, b2: BloqueioUsuario |
        b1.bloqueador != b2.bloqueador or b1.bloqueado != b2.bloqueado
    all t: SessaoToken | lone sessoes.t
    all disj tr1, tr2: TokenRevogado | tr1.token != tr2.token
    no TokenRevogado.token & Usuario.sessoes
    all u: Usuario | u.ativo = False implies no u.sessoes
}

fun usuariosAtivos: set Usuario { { u: Usuario | u.ativo = True } }
fun usuariosInativos: set Usuario { Usuario - usuariosAtivos }
fun tokensAtivos: set SessaoToken { Usuario.sessoes }
fun tokensRevogados: set SessaoToken { TokenRevogado.token }

fun bloqueadosPor[u: Usuario]: set Usuario {
    u.~bloqueador.bloqueado
}

pred existeBloqueio[u1, u2: Usuario] {
    some b: BloqueioUsuario | b.bloqueador = u1 and b.bloqueado = u2
}

pred bloqueioEntre[u1, u2: Usuario] {
    existeBloqueio[u1, u2] or existeBloqueio[u2, u1]
}

pred podeAutenticar[u: Usuario, t: SessaoToken] {
    u.ativo = True
    t not in tokensRevogados
    t not in tokensAtivos
}

pred podeEncerrarSessao[u: Usuario, t: SessaoToken] {
    t in u.sessoes
    t not in tokensRevogados
}

pred podeConsultarPerfilPublico[solicitante, alvo: Usuario] {
    solicitante.ativo = True
    alvo.ativo = True
    not bloqueioEntre[solicitante, alvo]
}

pred podeBloquear[u1, u2: Usuario] {
    u1 != u2
    no b: BloqueioUsuario | b.bloqueador = u1 and b.bloqueado = u2
}

assert SemAutoBloqueio {
    no b: BloqueioUsuario | b.bloqueador = b.bloqueado
}

assert BloqueioUnicoPorPar {
    all disj b1, b2: BloqueioUsuario |
        b1.bloqueador != b2.bloqueador or b1.bloqueado != b2.bloqueado
}

// Direta: consequência imediata de IntegridadeUsuario.
assert UsuarioInativoSemToken {
    all u: Usuario | u.ativo = False implies no u.sessoes
}

// Indireta: combina revogação, sessões e autenticação.
assert TokenRevogadoNuncaAutentica {
    all u: Usuario, t: tokensRevogados | not podeAutenticar[u, t]
}

// Indireta: bloqueio em qualquer direção corta a consulta pública.
assert BloqueioBidirecionalProtegePerfil {
    all disj u1, u2: Usuario |
        bloqueioEntre[u1, u2] implies not podeConsultarPerfilPublico[u1, u2]
}

check SemAutoBloqueio for 5
check BloqueioUnicoPorPar for 5
check UsuarioInativoSemToken for 5
check TokenRevogadoNuncaAutentica for 5
check BloqueioBidirecionalProtegePerfil for 5

run { some BloqueioUsuario and some u: Usuario | u.ativo = False } for 4
