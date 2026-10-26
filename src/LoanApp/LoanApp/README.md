# Loan Assessment App

A Java (Swing) desktop application that evaluates loan applications from
individuals and businesses, built as a learning project. The user interface is in Greek.

## Features

### Individuals
- Automatic credit score (300–850) from payment history, credit utilization,
  credit history length, recent inquiries and credit mix
- Approval or rejection with an interest rate based on credit tier, assets and loan purpose
  (investment, personal use, leisure); very poor credit history is rejected automatically

### Businesses
- Size classification under the Greek Accounting Standards (Law 4308/2014, thresholds as amended
  by Law 5164/2024): micro, small, medium, large
- Business credit score from financial ratios (solvency, liquidity, profitability, interest coverage,
  payment behaviour) and a debt-service coverage (DSCR) check
- Automatic reading of financial statements from PDF, TXT or CSV files, with optional OCR for
  scanned PDFs (Apache PDFBox and Tesseract)
- Company lookup by tax ID (VAT number) through the GEMI Open Data API, with a mock registry for demos

## Requirements
- Java 17 or newer
- Optional: Apache PDFBox (and Bouncy Castle for encrypted PDFs) to read PDF statements
- Optional: Tesseract OCR with Greek language data (`ell`) for scanned PDFs
- Optional: GEMI API key (environment variable `GEMI_API_KEY`)

## Run
Run the `Main` class.

## Status and limitations
- Educational project, not a real credit decision tool. Rates, limits and weights are illustrative assumptions.
- The GEMI API client has not been tested against the live API.
- Greek OCR accuracy has not been verified.