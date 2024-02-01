/*
 *  Copyright (C) 2022 Lamberto Colazzo
 *
 *  This file is part of the ConnectX software developed for the
 *  Intern ship of the course "Information technology", University of Bologna
 *  A.Y. 2021-2022.
 *
 *  ConnectX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This  is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details; see <https://www.gnu.org/licenses/>.
 */

package connectx.Ronaldo;

import connectx.*;
import connectx.Ronaldo.Coppia;

import java.util.*;


/**
 * Software player only a bit smarter than random.
 * <p>
 * It can detect a single-move win or loss. In all the other cases behaves
 * randomly.
 * </p>
 */
public class Ronaldo implements CXPlayer {

    //Limite di tempo (secondi)
    private int  TIMEOUT;
    //Tempo iniziale (millisecondi)
    private long START;

    // Numero di righe (M), colonne (N) e pezzi da allineare per vincere (K)
    private int M, N, K;

    // Stati del gioco che rappresentano la vittoria di questo giocatore e la vittoria dell'avversario
    private CXGameState vittoriaRonaldo;
    private CXGameState vittoriaAvversario;

    // Stato della cella rappresentanti il pezzo di questo giocatore
    private CXCellState cellaRonaldo;
    // Colonna della mossa migliore
    private int mossaMigliore;
    // Flag che indica se il tempo è scaduto
    private boolean tempoScaduto = false;


    // Array che contiene le valutazioni delle colonne, righe e diagonali
    private long[] lastColumnEvaluations;
    private long[] lastRowEvaluations;
    private long[] lastAscDiagEvaluations;
    private long[] lastDescDiagEvaluations;

    // Dimensioni dell'array delle diagonali
    private int Rdiag;
    private int Cdiag;
    private int DiagNum;

    // Distanza tra la prima riga e la prima diagonale ascendente
    private int HM;

    // Ultima valutazione della tavola
    private long lastEvaluation = 0L;

    // Ultima colonna e riga in cui è stata effettuata una mossa da Ronaldo
    private int lastColumn = -1;
    private int lastRow = -1;

    /* Default empty constructor */
    public Ronaldo() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {

        //Init dei parametri
        this.M = M;
        this.N = N;
        this.K = K;

        //Calcolo il numero di righe e colonne dei generatori di diagonali
        //Spiegazione: numero di righe e di colonne togliendo quelle da cui non possono partire diagonali lunghe K
        this.Rdiag = M - (K - 1);
        this.Cdiag = N - (K - 1);
        //Calcolo la dimensione del rettangolo di diagonali generatrici e ottengo la lunghezza dell'array di queste
        this.DiagNum = Rdiag * Cdiag;
        //Distanza tra la prima riga e la prima riga di diagonali ascendenti generatrici
        this.HM = (M - Rdiag);

        //Inizializzo gli array di valutazione
        this.subdiag_array = new CXCellState[K]; // Array that contains a diagonal of cells
        this.col_array = new CXCellState[M]; // Array that contains a column of cells
        this.row_array = new CXCellState[N]; // Array that contains a row of cells


        // Inizializzo gli array di cache di valutazione
        lastColumnEvaluations = new long[N];
        lastRowEvaluations = new long[M];
        lastAscDiagEvaluations = new long[this.DiagNum];
        lastDescDiagEvaluations = new long[this.DiagNum];


        // Setto gli stati di vittoria e lo stato della cella rappresentante il pezzo di questo giocatore
        // in base al fatto che gioco per primo o per secondo
        if (first) {
            this.vittoriaRonaldo = CXGameState.WINP1;
            this.vittoriaAvversario = CXGameState.WINP2;
            this.cellaRonaldo = CXCellState.P1;
        } else {
            this.vittoriaRonaldo = CXGameState.WINP2;
            this.vittoriaAvversario = CXGameState.WINP1;
            this.cellaRonaldo = CXCellState.P2;
        }

        // Setto il timeout
        this.TIMEOUT = timeout_in_secs;
    }


    /**
     * La funzione selectColumn avvia il timer e inizia
     * l'iterative deepening
     * che esegue una ricerca in profondità per
     * trovare la mossa migliore da effettuare.
     */
    public int selectColumn(CXBoard board) {
        tempoScaduto = false;
        //Salvo il tempo iniziale
        START = System.currentTimeMillis();

        // La miglior mossa iniziale è in mezzo alla griglia
        if (board.numOfMarkedCells() == 0) return N / 2;


        //Setto a true per utilizzaere una valutazione velocizzata
        isStartingEvaluation = true;
        // Valutazione corrente
        long currentEval = evaluate(board, 0);
        // Salvo la valutazione corrente
        lastEvaluation = currentEval;
        isStartingEvaluation = false;

        //Setto i valori di alpha e beta a min e max perchè non ho ancora valutato nessuna mossa
        long alpha = Long.MIN_VALUE + 1;
        long beta = Long.MAX_VALUE - 1;
        //Variabile che salva la mossa migliore trovata fino ad ora
        int prev;
        int profonditaCorrente = 0;

        this.mossaMigliore = -1;

        //ricerca in profondità
        int maxDepth = 8;

        long bestEval = 0L;
        long lastEval = 0L;

        //Finchè non scade il tempo e non raggiungo la profondità massima continuo a cercare aumentando la profondità
        while(!tempoScaduto && profonditaCorrente <= maxDepth) {
            //Salvo la mossa migliore trovata fino ad ora
            prev = this.mossaMigliore;
            //Eseguo la ricerca in profondità con la profondità corrente
            lastEval = minimax(board, currentEval, 0, profonditaCorrente, alpha, beta, true);
            //Se il tempo è scaduto non ho potuto stabilire una mossa migliore affidabile con profondità = d, quindi uso quella precedente
            if(tempoScaduto) {
                System.out.println("Tempo scaduto, profondità raggiunta: " + profonditaCorrente);
                this.mossaMigliore = prev;
            }

            if (lastEval > bestEval) {
                bestEval = lastEval;
            }

            // Incremento la profondità
            profonditaCorrente++;
        }

        //Salvo la mossa scelta
        lastColumn = mossaMigliore;

        //Ottengo la riga della mossa
        for(int i = M-1; i >= 0; i--) {
            if(board.cellState(i, mossaMigliore) == CXCellState.FREE) {
                //Salvo la riga della mossa
                lastRow = i;
                break;
            }
        }

        //Restituisco la mossa migliore
        return this.mossaMigliore;
    }


    // Utility per convertire i millisecondi in secondi
    private long millisToSec(long millis) {
        return millis / 1000;
    }


    //Utility per controllare se il tempo è scaduto
    private boolean timeIsRunningOut() {
        // Tempo corrente - tempo iniziale >= 95% del timeout
        //95% perchè voglio un po di margine per stoppare il programma e scegliere la mossa migliore fin ora
        return (millisToSec(System.currentTimeMillis() - START) >= TIMEOUT * (95.0 / 100.0));
    }


    private long minimax(CXBoard B, long current_eval, int depthCurrent, int depthMax, long alpha, long beta, boolean maximizing) {

        //Situazione terminale, si è raggiunta una foglia dell'albero di ricerca o si è raggiunta la profondità massima
        if (depthCurrent == depthMax || B.gameState() != CXGameState.OPEN || B.numOfFreeCells() == 0)
            return evaluate(B, depthCurrent);

        // Massimizzo il mio punteggio
        if (maximizing) {
            // Inizializzo il valore della mossa migliore a min
            long val = Long.MIN_VALUE + 1;
            Integer[] L = B.getAvailableColumns();
            //Array di coppie (mossa, valore) che verranno ordinati in ordine decrescente
            List<Coppia<Integer, Long>> orderedMoves = new ArrayList<>();

            //Qui vengono calcolati i valori delle mosse disponibili
            for (int i : L) {
                long childHeuristic = update_evaluate(B, depthCurrent, current_eval, i);
                orderedMoves.add(new Coppia<>(i, childHeuristic));
            }
            //Sort the moves in decreasing order (la prima ha la valutazione più alta)
            orderedMoves.sort((a, b) -> b.second.compareTo(a.second));

            // Itero sulle mosse scelte
            for (Coppia<Integer, Long> j : orderedMoves) {
                // Ottengo la mossa
                Integer i = j.first;

                if (timeIsRunningOut()) {
                    this.tempoScaduto = true;
                    return val;
                }

                //Eseguo la minimax sulla mossa minimizzando il punteggio dell'avversario
                B.markColumn(i);
                long childVal = minimax(B, j.second, depthCurrent+1, depthMax, alpha, beta, false);
                B.unmarkColumn();

                // Aggiorno il valore della mossa migliore se ho trovato una mossa migliore
                if (childVal > val) { //Maximize
                    val = childVal;

                    //Se la profondità corrente è 0, allora sono nel nodo radice, quindi devo aggiornare la mossa migliore
                    //Ritorniamo qui dopo la ricorrenza che ha ripassato il controllo alla chiamata di minimax con currentDepth = 0
                    if (depthCurrent == 0)  {
                        //If true we are in the root node, so we should update the bestMove variable.
                        mossaMigliore = i;
                    }
                }

                //Aggiorno alpha
                alpha = Math.max(alpha, val);
                if (beta <= alpha) //Pruning
                    break;
            }
            return val;
        }

        else {
            //Inizializzo il valore della mossa migliore a max
            long val = Long.MAX_VALUE - 1;
            Integer[] L = B.getAvailableColumns();
            //Array di coppie (mossa, valore) che verranno ordinati in ordine decrescente
            List<Coppia<Integer, Long>> orderedMoves = new ArrayList<>();

            //Qui vengono calcolati le valutazioni delle mosse disponibili
            for (int i : L) {
                long childEval = update_evaluate(B, depthCurrent, current_eval, i);
                orderedMoves.add(new Coppia<>(i, childEval));
            }
            //ordinamento in ordine crescente
            orderedMoves.sort((a, b) -> -b.second.compareTo(a.second));

            for (Coppia<Integer, Long> j : orderedMoves) {
                int i = j.first;

                if (timeIsRunningOut()) {
                    this.tempoScaduto = true;
                    return val;
                }

                B.markColumn(i);
                val = Math.min(val, minimax(B, j.second, depthCurrent+1, depthMax, alpha, beta, true)); //Minimize
                B.unmarkColumn();

                beta = Math.min(beta, val);
                if (beta <= alpha) //Pruning
                    break;
            }
            return val;
        }
    }

    private boolean isStartingEvaluation = false;

    //Inizializziamo gli array di valutazione
    CXCellState[] subdiag_array;
    CXCellState[] col_array;
    CXCellState[] row_array;


    void updateDescendingDiagonalEvaluations(int index, CXCellState[][] board) {
        //Ri-ottengo la colonna della diagonale con la formula Colonna = Indice % NumeroColonneGeneratori
        int c = index % Cdiag;
        //Ri-ottengo la riga della diagonale con la formula Riga = (Indice - Colonna) / NumeroColonneGeneratori
        //(formula inversa rispetto a quella degli indici)
        int r = (index - c) / Cdiag;

        //Per ogni elemento della diagonale creo un array che contiene gli elementi della diagonale
        for (int i = 0; i < K; i++)
            subdiag_array[i] = board[r + i][c + i];

        // Valuto la diagonale e aggiorno la valutazione corrente
        long diag_score = eval_sub(subdiag_array, 0, K);

        // Rimuove la valutazione precedente della diagonale;
        lastEvaluation -= lastDescDiagEvaluations[index];
        // Inserisci la nuova valutazione della diagonale
        lastDescDiagEvaluations[index] = diag_score;
        // Aggiorna la valutazione corrente
        lastEvaluation += lastDescDiagEvaluations[index];
    }

    void updateAscendingDiagonalEvaluations(int index, CXCellState[][] board) {
        //Ri-ottengo la colonna della diagonale con la formula Colonna = Indice % NumeroColonneGeneratori
        int c = index % Cdiag;
        //Ri-ottengo la riga della diagonale con la formula Riga = (Indice - Colonna) / NumeroColonneGeneratori + HM
        //(formula inversa rispetto a quella degli indici)
        int r = (index - c) / Cdiag + HM;

        //Per ogni elemento della diagonale creo un array che contiene gli elementi della diagonale
        for (int i = 0; i < K; i++)
            subdiag_array[i] = board[r - i][c + i];
        // Valuto la diagonale e aggiorno la valutazione corrente
        long diag_score = eval_sub(subdiag_array, 0, K);

        // Rimuove la valutazione precedente della diagonale;
        lastEvaluation -= lastAscDiagEvaluations[index];
        // Inserisci la nuova valutazione della diagonale
        lastAscDiagEvaluations[index] = diag_score;
        // Aggiorna la valutazione corrente
        lastEvaluation += lastAscDiagEvaluations[index];
    }

    void updateDiagonalEvaluations(int tmpRow, int tmpCol, CXCellState[][] board, boolean isDescending) {
        if (isDescending) {
            do {
                //Calcoliamo l'indice della diagonale con la formula Riga * NumeroColonneGeneratori + Colonna
                //Moltiplicando R per Cdiag diamo un peso al valore in base alla riga in cui si trova
                //Aggiungendo C lo spostiamo in base alla colonna in cui si trova
                //Proietta la mossa sull'indice della diagonale
                int possibleIndex = tmpCol + (Cdiag * tmpRow);

                //Controlla che l'indice appartenga a quelli dei generatori
                if (possibleIndex < DiagNum) {
                    //Calcola  la valutazione della diagonale e aggiorna la valutazione corrente
                    updateDescendingDiagonalEvaluations(possibleIndex, board);
                }
                //Decrementa per risalire la diagonale al suo possibile generatore
                tmpRow--;
                tmpCol--;
            }
            //Controlla che non si esca dalla griglia
            while (tmpRow >= 0 && tmpCol >= 0);
        } else  {
            do {
                //Calcoliamo l'indice della diagonale con la formula (Riga - (Distanza tra l'ultima riga e la prima riga di generatori di diagonali ascendenti)) * NumeroColonneGeneratori + Colonna
                //Stessa motivazione di prima ma shiftiamo le righe sopra alla tavola per lavorare sempre con indici che vanno da 0 a DiagNum
                //Proietta la mossa sull'indice della diagonale
                int possibleIndex = ((tmpRow - HM) * Cdiag) + tmpCol;
                //Controlla che l'indice appartenga a quelli dei generatori e che sia positivo
                if (possibleIndex < DiagNum && possibleIndex > 0) {
                    updateAscendingDiagonalEvaluations(possibleIndex, board);
                }
                //Sali con la riga e scendi con la colonna per risalire al generatore della diagonale ascendente
                tmpRow++;
                tmpCol--;
            }
            //Controlla che non si esca dalla griglia
            while (tmpRow < M && tmpCol >= 0);
        }
    }

    long updateColumnEvaluations(Integer[] cols, CXCellState[][] board, boolean[] isEmptyRow) {
        // Vertical alignments
        long score = 0L;

        // Per ogni colonna (possono essere tutte o solo alcune selezionate)
        for (int move : cols) {

            //Controllo quali righe sono vuote
            for (int d = 0; d < M; d++) {
                col_array[d] = board[d][move]; // col_array now contains the current column
                if (col_array[d] != CXCellState.FREE) isEmptyRow[d] = false; //If board[d][lastMoves[i]] contains a piece, row d is not empty
            }

            long col_score = 0L;
            //Se la riga più in basso è vuota, la colonna è vuota, quindi la ignoro
            if (col_array[M - 1] == CXCellState.FREE) continue;

            //Controlla ogni sottosequenza di K elementi a partire dalla posizione r (primo generatore di sequenza in colonna) fino ad arrivare a 0
            for (int r = M - K; r >= 0; r--) {
                // Se la cella più in basso della sottocolonna è vuota, la sottocolonna è vuota, quindi la ignoro
                if (col_array[r + K - 1] == CXCellState.FREE) break;
                //Calcola lo score della sottocolonna e aggiorna lo score della colonna
                long subSeqScore = eval_sub(col_array, r, r + K);
                col_score += subSeqScore;
            }

            if (isStartingEvaluation) {
                //Rimuove la valutazione precedente della colonna;
                lastEvaluation -= lastColumnEvaluations[move];
                //Inserisci la nuova valutazione della colonna
                lastColumnEvaluations[move] = col_score;
                //Aggiorna la valutazione corrente
                lastEvaluation += lastColumnEvaluations[move];
            } else {
                score += col_score;
            }
        }

        return score;
    }

    long updateRowEvaluations(Integer[] rows, CXCellState[][] board, boolean[] isEmptyRow) {
        // Horizontal alignments
        long score = 0L;

        //Per ogni riga (possono essere tutte o solo alcune selezionate)
        for (int row : rows) {
            //Se la riga è vuota, la ignoro
            if(isEmptyRow[row]) break;

            //Contiene l'array della riga corrente
            row_array = board[row];
            //Inizializzo lo score della riga corrente a 0
            long row_score = 0L;

            //Trovo le sottosequenze della riga partendo da 0 fino a N-K
            for (int c = 0; c < N - (K - 1); c++) {
                // Valuto la sottosequenza di K elementi a partire dalla posizione c fino a c+K
                long subRowScore = eval_sub(row_array, c, c + K); // Increment the current score by the score assigned to the
                //aggiorno lo score della riga corrente
                row_score += subRowScore;
            }
            if (isStartingEvaluation) {
                //Rimuove la valutazione precedente della riga;
                lastEvaluation -= lastRowEvaluations[row];
                //Inserisci la nuova valutazione della riga
                lastRowEvaluations[row] = row_score;
                //Aggiorna la valutazione corrente
                lastEvaluation += lastRowEvaluations[row];
            } else {
                score += row_score;
            }
        }

        return score;
    }

    private long evaluate(CXBoard B, int depth) {

        //Situazione terminale
        if (B.gameState() != CXGameState.OPEN) {
            //Se ho vinto, ritorno un valore molto alto
            if (B.gameState() == vittoriaRonaldo)
                return Long.MAX_VALUE - 1 - depth;
            //Se ha vinto l'avversario, ritorno un valore molto basso
            else if (B.gameState() == vittoriaAvversario)
                return Long.MIN_VALUE + 1 + depth;
            //Se è un pareggio, ritorno 0
            else
                return 0L;
        }

        //Matrice della tavola di gioco
        CXCellState[][] board = B.getBoard();
        boolean[] isEmptyRow = new boolean[M];
        long score = 0L;

        //Inizializzo tutte le righe come vuote
        for(int i = 0; i < M; i++) isEmptyRow[i] = true;

        if (isStartingEvaluation && lastColumn != -1) {

            //get opponent move
            CXCell lastMove = B.getLastMove();
            int opponentCol = lastMove.j;
            int opponentRow = lastMove.i;

            //Column update
            //Se la mossa dell'avversario è stata fatta nella stessa colonna della mossa precedente, aggiorno solo quella colonna
            Integer[] lastColumns = opponentCol == lastColumn ? new Integer[] {opponentCol} : new Integer[] {opponentCol, lastColumn};
            updateColumnEvaluations(lastColumns, board, isEmptyRow);

            //Row update
            //Se la mossa dell'avversario è stata fatta nella stessa riga della mossa precedente, aggiorno solo quella riga
            Integer[] lastRows = opponentRow == lastRow ? new Integer[] {opponentRow} : new Integer[] {opponentRow, lastRow};
            Arrays.sort(lastRows, Collections.reverseOrder());
            updateRowEvaluations(lastRows, board, isEmptyRow);

            //DIAGONALS update sia discendenti che ascendenti.
            updateDiagonalEvaluations(lastRow, lastColumn, board, true);
            updateDiagonalEvaluations(lastRow,lastColumn,board,false);
            //Se la mossa dell'avversario è stata fatta in una colonna diversa da quella precedente, aggiorno anche quella diagonale
            if (opponentCol != lastColumn) {
                updateDiagonalEvaluations(opponentRow, opponentCol, board, true);
                updateDiagonalEvaluations(opponentRow,opponentCol,board,false);
            }

            //Aggiorno lo score in base alle modifiche fatte
            score = lastEvaluation;

        } else {

            //Aggiorno le colonne
            //Aggiorno lo score direttamente solo se non sto facendo la valutazione iniziale, altrimenti
            //aggiorno l'array di cache o comunque aggiorno dentro la funzione
            long newColScore = updateColumnEvaluations(B.getAvailableColumns(), board, isEmptyRow);
            if (!isStartingEvaluation)
                score += newColScore;

            //Creo un array di righe dalla più alta alla più bassa (dal basso all'alto nella tavola)
            Integer[] allRows = new Integer[M];
            for(int i = M-1; i >= 0; i--) {
                allRows[i] = i;
            }

            //Aggiorno le righe
            //Aggiorno lo score direttamente solo se non sto facendo la valutazione iniziale, altrimenti
            //aggiorno l'array di cache o comunque aggiorno dentro la funzione
            long newRowScore = updateRowEvaluations(allRows, board, isEmptyRow);
            if (!isStartingEvaluation)
                score += newRowScore;



            /* INDICI DELLE DIAGONALI
             * Gli indici delle diagonali discendenti sono calcolati con la formula:
             * Indice = Riga * NumeroColonneGeneratori + Colonna
             * Questo serve a proiettare la cella sull'indice della diagonale
             *
             * Per le diagonali ascendenti invece la formula è:
             * Indice = (Riga - (Distanza tra l'ultima riga e la prima riga di generatori di diagonali ascendenti)) * NumeroColonneGeneratori + Colonna
             */

            // Descending diagonals
            //Itero per righe e colonne dove posso trovare diagonali discendenti
            //Escludendo le celle negli angoli in alto a destra e in basso a sinistra
            for (int r = 0; r < Rdiag; r++) {
                for (int c = 0; c < Cdiag; c++) {

                    // Per ogni generatore della diagonale riempio un array che contiene gli elementi della diagonale
                    for (int i = 0; i < K; i++)
                        subdiag_array[i] = board[r + i][c + i]; // subdiag_array now contains the elements of the current

                    // Valuto la diagonale
                    long subSeqScore = eval_sub(subdiag_array, 0, K); // Increment the current score by the score assigned to the
                    score += subSeqScore;

                    //Aggioorno l'array di cache se sto facendo la valutazione iniziale
                    if (isStartingEvaluation) {
                        lastDescDiagEvaluations[c + (Cdiag * r)] = subSeqScore;
                    }
                }
            }


            //Ascending diagonals
            //Itero per righe e colonne dove posso trovare diagonali ascendenti
            //Escludendo le celle negli angoli in alto a sinistra e in basso a destra
            for (int r = K -1; r < M; r++) {
                for(int c = 0; c < N - (K - 1); c++) {

                    // Per ogni generatore della diagonale riempio un array che contiene gli elementi della diagonale
                    for (int i = 0; i < K; i++)
                        subdiag_array[i] = board[r - i][c + i];
                    // Valuto la diagonale
                    long subSeqScore = eval_sub(subdiag_array, 0, K);
                    // Aggioorno lo score
                    score += subSeqScore;

                    //Aggioorno l'array di cache se sto facendo la valutazione iniziale
                    if (isStartingEvaluation) {
                        lastAscDiagEvaluations[(r - HM) * Cdiag + c] = subSeqScore;
                    }
                }
            }




        }

        return score;
    }


    private long update_subsequences(long lastEval, boolean adding, int row, int col, CXCellState[][] board) {

        //Prende la riga e la colonna della mossa e calcola gli indici di inizio e fine
        //delle sottosequenze in cui la mossa è contenuta, le funzioni min e max servono
        //per non uscire dalla board
        int verticalStart = Math.max(row - (K-1), 0);
        int verticalEnd = Math.min(row + (K-1), M-1);
        int horizontalStart = Math.max(col - (K-1), 0);
        int horizontalEnd = Math.min(col + (K-1), N-1);

        CXCellState[] current_sub = new CXCellState[K];

        //Sequenze verticali
        //Itera per le sottosequenze di colonne che contengono la mossa
        for(int i = verticalStart; i+K-1 <= verticalEnd; i++){
            //Crea un array che contiene gli elementi della sottosequenza
            for(int r = i; r < i+K; r++) current_sub[r-i] = board[i][col];
            //Rimuove la valutazione della sottosequenza
            lastEval += (adding ? 1 : -1) * eval_sub(current_sub, 0, K);
        }

        //Sequenze orizzontali
        //Itera per le sottosequenze di righe che contengono la mossa
        for(int j = horizontalStart; j+K-1 <= horizontalEnd; j++){
            //Crea un array che contiene gli elementi della sottosequenza
            current_sub = Arrays.copyOfRange(board[row], j, j+K);
            //Rimuove la valutazione della sottosequenza
            lastEval += (adding ? 1 : -1) * eval_sub(current_sub, 0, K);
        }

        //Sequence diagonali discendenti
        //Itera per le sottosequenze di diagonali discendenti che contengono la mossa
        for(int i = verticalStart, j = horizontalStart; i+K-1 <= verticalEnd && j+K-1 <= horizontalEnd; i++, j++){
            //Crea un array che contiene gli elementi della sottosequenza
            for(int c = 0; c < K; c++) current_sub[c] = board[i+c][j+c];
            //Rimuove la valutazione della sottosequenza
            lastEval += (adding ? 1 : -1) * eval_sub(current_sub, 0, K);
        }

        //Sequenze diagonali ascendenti
        //Itera per le sottosequenze di diagonali ascendenti che contengono la mossa
        for(int i = verticalEnd, j = horizontalStart; i-K+1 >= verticalStart && j+K-1 <= horizontalEnd; i--, j++){
            //Crea un array che contiene gli elementi della sottosequenza
            for(int c = 0; c < K; c++) current_sub[c] = board[i-c][j+c];
            //Rimuove la valutazione della sottosequenza
            lastEval += (adding ? 1 : -1) * eval_sub(current_sub, 0, K);
        }

        return lastEval;
    }


    /*
     * Questa funzione aggiorna la valutazione corrente della tavola di gioco basandosi
     * su delle euistiche che valutano le sottosequenze di K elementi in cui la mossa
     * è contenuta.
     */
    private long update_evaluate(CXBoard B, int depth, long lastEval, int col){
        B.markColumn(col);

        //Se la partita è finita o non ci sono più celle libere, ritorna la valutazione
        if((B.gameState() != CXGameState.OPEN) || (B.numOfFreeCells() == 0)){
            // Se lo stato è terminale la valutazione è calcolata in O(1) da eval()
            long score = evaluate(B, depth+1);
            B.unmarkColumn();
            return score;
        }

        //Ottiene la riga della mossa
        CXCell newMove = B.getLastMove();
        int row = newMove.i;
        B.unmarkColumn();

        //Matrice della tavola di gioco
        CXCellState[][] board = B.getBoard();

        /*
         * Qui vengono rimosse dalla valutazione le sottosequenze della tavola prima di
         * contentere la nuova mossa.
         */
        lastEval = update_subsequences(lastEval, false, row, col, board);


        /*
         * Qui vengono aggiunte alla valutazione le sottosequenze della tavola dopo
         * aver aggiunto la nuova mossa.
         */
        B.markColumn(col);
        lastEval = update_subsequences(lastEval, true, row, col, board);
        B.unmarkColumn();

        return lastEval;
    }

    private int eval_sub(CXCellState[] arr, int start, int end) {
        //Counter per i miei pezzi e quelli dell'avversario
        int count_mine = 0, count_yours = 0;
        int score = 0;

        //Conto in una sequenza quante celle sono mie e quante sono dell'avversario
        for (int i = start; i < end; i++) {
            if (arr[i] == cellaRonaldo)
                count_mine++;

            else if (arr[i] != CXCellState.FREE)
                count_yours++;
        }

        //Incremento lo score in base alla differenza tra i miei pezzi e quelli dell'avversario
        score += (count_mine - count_yours);


        /*
            Controlla se la sottosequenza è vincente o perdente, se io non ho pezzi o se l'avversario non ha pezzi
            si eleva al quadrato per dare più importanza alla sottosequenza, esempio, in una sotto sequenza di 4 l'avversario
            ha 3 pezzi, quindi questa colonna va scelta ASSOLUTAMENTE per bloccargli il quarto
         */
        if (count_yours == 0)
            score *= (int) Math.pow(2, count_mine);
        if (count_mine == 0)
            score *= (int) Math.pow(2, count_yours);

        return score;
    }

    public String playerName() {
        return "Ronaldo";
    }
}
