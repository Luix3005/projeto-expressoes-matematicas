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
  valorX: number | null = null;

  prepararEdicao(item: Expression) {
    this.idEmEdicao = item.id ?? null;
    this.novaConta = item.expression;
    
  }

  btnCalcular() {
    if (!this.novaConta.trim()) return;

    if (this.idEmEdicao) {
      
      this.service.editar(this.idEmEdicao, this.novaConta, this.valorX ?? undefined).subscribe({
        next: () => this.finalizarAcao(),
        error: (erro) => console.error('Erro ao editar:', erro)
      });
    } else {
      
      this.service.salvar(this.novaConta, this.valorX ?? undefined).subscribe({
        next: () => this.finalizarAcao(),
        error: (erro) => {
          console.error('Erro ao salvar:', erro);
          alert('Erro ao calcular. Verifique a sintaxe ou a conexão.');
        }
      });
    }
  }
  
  private finalizarAcao() {
    this.novaConta = '';
    this.idEmEdicao = null;
    this.valorX = null;
    window.location.reload(); 
  }
}