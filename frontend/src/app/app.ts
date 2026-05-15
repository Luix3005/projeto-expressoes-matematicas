import { Component, inject } from '@angular/core';
import { Historico } from './components/historico/historico';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ExpressionService } from './services/expression.service';
import { Expression } from './models/expression.model';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, Historico],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class AppComponent {
  private service = inject(ExpressionService);
  
  novaConta: string = '';
  idEmEdicao: number | null = null;
  
  variaveisMap: { [key: string]: number } = {}; 
  nomesVariaveis: string[] = [];

  detectarVariaveis() {
    const regex = /[a-zA-Z]+/g;
    const encontradas = this.novaConta.match(regex) || [];
    
    this.nomesVariaveis = [...new Set(encontradas)];

    this.nomesVariaveis.forEach(nome => {
      if (this.variaveisMap[nome] === undefined) {
        this.variaveisMap[nome] = 0;
      }
    });

    Object.keys(this.variaveisMap).forEach(key => {
      if (!this.nomesVariaveis.includes(key)) {
        delete this.variaveisMap[key];
      }
    });
  }

  prepararEdicao(item: Expression) {
    this.idEmEdicao = item.id ?? null;
    this.novaConta = item.expression;
    this.detectarVariaveis();
  }

  btnCalcular() {
    if (!this.novaConta.trim()) return;

    if (this.idEmEdicao) {
      this.service.editar(this.idEmEdicao, this.novaConta, this.variaveisMap).subscribe({
        next: () => this.finalizarAcao(),
        error: (erro) => console.error('Erro ao editar:', erro)
      });
    } else {
      this.service.salvar(this.novaConta, this.variaveisMap).subscribe({
        next: () => this.finalizarAcao(),
        error: (erro) => {
          console.error('Erro ao salvar:', erro);
          alert('Erro ao calcular. Verifique se todas as letras têm valores.');
        }
      });
    }
  }
  
  private finalizarAcao() {
    this.novaConta = '';
    this.idEmEdicao = null;
    this.variaveisMap = {};
    this.nomesVariaveis = [];
    window.location.reload(); 
  }
}