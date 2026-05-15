import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Expression } from '../models/expression.model';

interface ExpressionPage {
  content: Expression[];
  totalElements: number;
}

@Injectable({
  providedIn: 'root',
})
export class ExpressionService {
  private apiUrl = 'http://localhost:8080/api/calculator';

  constructor(private http: HttpClient) {}

  listar(pagina: number, itensPorPagina: number, termo?: string, data?: string, criador?: string): Observable<ExpressionPage> {
    let params = new HttpParams()
      .set('page', pagina.toString())
      .set('size', itensPorPagina.toString())
      .set('sort', 'createdAt,desc');

    if (termo) params = params.set('termo', termo);
    if (data) params = params.set('dataStr', data);
    if (criador) params = params.set('criador', criador);

    return this.http.get<ExpressionPage>(`${this.apiUrl}/todos`, { params });
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  salvar(expressao: string, variaveis: { [key: string]: number }): Observable<Expression> {
    const corpo = {
      expressao: expressao,
      variaveis: variaveis
    };

    // No POST, o segundo parâmetro é o corpo (body) da requisição
    return this.http.post<Expression>(this.apiUrl, corpo);
  }

  editar(id: number, expressao: string, variaveis: { [key: string]: number }): Observable<Expression> {
    const corpo = {
      expressao: expressao,
      variaveis: variaveis
    };

    return this.http.put<Expression>(`${this.apiUrl}/${id}`, corpo);
  }
}