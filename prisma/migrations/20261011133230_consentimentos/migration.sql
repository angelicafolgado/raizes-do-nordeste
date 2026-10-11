-- CreateTable
CREATE TABLE "consentimentos" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "usuarioId" INTEGER NOT NULL,
    "finalidade" TEXT NOT NULL,
    "versaoAviso" TEXT NOT NULL,
    "aceito" BOOLEAN NOT NULL,
    "demonstracao" BOOLEAN NOT NULL DEFAULT true,
    "registradoEm" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "consentimentos_usuarioId_fkey" FOREIGN KEY ("usuarioId") REFERENCES "usuarios" ("id") ON DELETE RESTRICT ON UPDATE CASCADE
);

-- CreateIndex
CREATE INDEX "consentimentos_usuarioId_finalidade_id_idx" ON "consentimentos"("usuarioId", "finalidade", "id");
