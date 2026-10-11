import { Router } from 'express';
import { PrivacidadeController } from '../controllers/PrivacidadeController.js';
import { authMiddleware } from '../middlewares/authMiddleware.js';
export const privacidadeRoutes = Router();
privacidadeRoutes.get('/aviso', PrivacidadeController.aviso);
privacidadeRoutes.get('/consentimentos', authMiddleware(), PrivacidadeController.consultar);
privacidadeRoutes.put('/consentimentos/fidelidade', authMiddleware(), PrivacidadeController.registrar);
