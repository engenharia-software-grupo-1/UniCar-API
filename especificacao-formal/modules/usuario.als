module modules/usuario

sig Nome, Matricula, CPF, Email, Curso, SessaoToken {}

abstract sig Genero {}
one sig Masculino, Feminino, NaoInformado extends Genero {}

abstract sig Bool {}
one sig True, False extends Bool {}

sig Usuario {
    nome: one Nome,
    matricula: one Matricula,
    cpf: one CPF,
    email: one Email,
    curso: one Curso,
    genero: one Genero,
    ativo: one Bool,
    receberEmail: one Bool,
    sessoes: set SessaoToken
}

sig TokenRevogado { token: one SessaoToken }

sig BloqueioUsuario {
    bloqueador: one Usuario,
    bloqueado: one Usuario
}

fact IntegridadeUsuario {
    all disj u1, u2: Usuario | {
        u1.matricula != u2.matricula
        u1.cpf != u2.cpf
        u1.email != u2.email
    }
    all b: BloqueioUsuario | b.bloqueador != b.bloqueado
    all disj b1, b2: BloqueioUsuario |
        b1.bloqueador != b2.bloqueador
        or b1.bloqueado != b2.bloqueado
    all t: SessaoToken | lone sessoes.t
    all disj tr1, tr2: TokenRevogado | tr1.token != tr2.token
    no TokenRevogado.token & Usuario.sessoes
    all u: Usuario | u.ativo = False implies no u.sessoes
}

pred autenticado[u: Usuario] {
    u.ativo = True
    some u.sessoes
}

pred bloqueioEntre[u1, u2: Usuario] {
    some b: BloqueioUsuario |
        (b.bloqueador = u1 and b.bloqueado = u2)
        or (b.bloqueador = u2 and b.bloqueado = u1)
}

pred podeBloquear[autor, alvo: Usuario] {
    autenticado[autor]
    autor != alvo
    no b: BloqueioUsuario |
        b.bloqueador = autor and b.bloqueado = alvo
}

run { some u: Usuario | autenticado[u] } for 4
