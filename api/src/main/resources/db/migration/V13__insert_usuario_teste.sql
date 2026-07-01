-- Inserir usuário de teste para desenvolvimento
INSERT INTO usuario (matricula, nome, cpf, email, curso, genero, receber_email, ativo)
VALUES ('2024001', 'Usuário Teste', '12345678901', 'teste@example.com', 'Engenharia de Software', 'M', true, true)
ON CONFLICT DO NOTHING;
