export interface Expression {
  id?: number;
  expression: string;
  result: number | null;
  createdAt: string;
  lastExecutedAt: string;
  createdBy: string;
}
