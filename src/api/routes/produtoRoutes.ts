import { Router } from 'express';
import { ProdutoController } from '../controllers/ProdutoController.js';
export const produtoRoutes = Router();
produtoRoutes.get('/', ProdutoController.listar);
