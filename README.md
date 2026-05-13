# Calculadora de Expressoes

Projeto full stack para calcular expressoes matematicas e manter historico de resultados.

## Estrutura

- `backend/`: API REST em Java com Spring Boot, JPA e banco H2 local.
- `frontend/`: aplicacao Angular que consome a API.
- `GUIA_DE_ESTUDO_EXPRESSOES.md`: material de estudo do projeto.

## Como rodar

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

No Windows:

```bash
cd backend
mvnw.cmd spring-boot:run
```

A API fica disponivel em `http://localhost:8080/api/calculator`.

### Frontend

```bash
cd frontend
npm install
npm start
```

O Angular abre em `http://localhost:4200`.

## Banco local

O H2 grava os dados em `backend/data/` quando o backend e executado a partir da pasta `backend`. Essa pasta e local e nao deve ser enviada ao GitHub.

## Preparar para o GitHub

```bash
git init
git add .
git commit -m "Organiza projeto full stack"
git branch -M main
git remote add origin URL_DO_SEU_REPOSITORIO
git push -u origin main
```

