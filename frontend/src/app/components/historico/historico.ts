import { ChangeDetectorRef, Component, EventEmitter, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ExpressionService } from '../../services/expression.service';
import { Expression } from '../../models/expression.model';

@Component({
  selector: 'app-historico',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './historico.html',
  styleUrl: './historico.scss',
})
export class Historico implements OnInit {
  listaDeContas: Expression[] = [];
  paginaAtual = 0;
  itensPorPagina = 10;
  
  termoBusca: string = '';
  dataFiltro: string = ''; 
  criadorFiltro: string = ''; 
  expressaoInput: string = '';
  valorXInput: number | null = null;

  @Output() aoEditar = new EventEmitter<Expression>();

  constructor(
    private service: ExpressionService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.carregarDados();
  }

  
  filtrar(): void {
    this.paginaAtual = 0;
    this.carregarDados();
  }

  carregarDados(): void {
    this.service.listar(
      this.paginaAtual, 
      this.itensPorPagina, 
      this.termoBusca, 
      this.dataFiltro, 
      this.criadorFiltro
    ).subscribe({
      next: (dados) => {
        this.listaDeContas = dados.content;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erro ao buscar dados:', err)
    });
  }
  editar(item: Expression): void {
    console.log('1. Filho: Clique detectado para o item:', item);
    this.aoEditar.emit(item);
    console.log('2. Filho: Evento emitido!');
  }

  excluir(id: number | undefined): void {
    if (id && confirm('Tem certeza que deseja excluir este cálculo?')) {
      this.service.excluir(id).subscribe({
        next: () => {
          this.carregarDados();
        },
        error: (err) => console.error('Erro ao excluir:', err),
      });
      
    }
  }
  proximaPagina() {
    this.paginaAtual++;
    this.carregarDados();
  }

  paginaAnterior() {
    if (this.paginaAtual > 0) {
      this.paginaAtual--;
      this.carregarDados();
  }
  }

  executar(item: Expression) {
  if (item.id && item.expression) {
    console.log('Recalculando:', item.expression);
    
    this.service.editar(item.id, item.expression).subscribe({
      next: (resultadoAtualizado) => {
        console.log('Recalculado com sucesso:', resultadoAtualizado);
        this.carregarDados();
      },
      error: (err) => {
        console.error('Erro ao executar expressão:', err);
        alert('Erro ao calcular a expressão. Verifique a sintaxe.');
      }
    });
  }
}
salvarNovaExpressao() {
  if (this.expressaoInput) {
    // Chama o salvar passando a expressão e o X
    this.service.salvar(this.expressaoInput, this.valorXInput ?? undefined).subscribe({
      next: () => {
        this.expressaoInput = '';
        this.valorXInput = null;
        this.carregarDados();
      }
    });
  }
}
}
