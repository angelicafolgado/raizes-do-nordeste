import { Router } from 'express';
import { PedidoController } from '../controllers/PedidoController.js';
import { authMiddleware } from '../middlewares/authMiddleware.js';
export const pedidoRoutes = Router();
pedidoRoutes.post('/', authMiddleware(['CLIENTE', 'ATENDENTE', 'ADMIN']), PedidoController.criar);
pedidoRoutes.get('/', authMiddleware(), PedidoController.listar);
pedidoRoutes.post('/:id/pagamento', authMiddleware(['CLIENTE', 'ATENDENTE', 'ADMIN']), PedidoController.simularPagamento);
pedidoRoutes.patch('/:id/status', authMiddleware(['COZINHA', 'ADMIN']), PedidoController.atualizarStatus);
