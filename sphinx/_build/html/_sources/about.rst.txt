About TFpredict
================

TFpredict is a tool which implements a novel three-step classification method which expects a protein sequence as input and (1) distinguishes transcription factors (TF) from other proteins (Non-TF), (2) predicts the structural superclass of TFs (see TransFac classification), and (3) identifies the DNA-binding domains of TFs. The latter two classification steps are only to be performed if the given protein sequence was identified as a TF. The tool incorporates the results from a `BLAST+`_ search into a novel feature representation which allows TF/non-TF classification by state-of-the-art machine learning methods. Specific supervised classifiers were constructed for the task of identifying TFs and their structural superclasses, respectively. Next, known protein domains are detected by the tool `InterProScan`_, and then the DNA-binding domains among these are filtered through GO-terms. TFpredict was implemented as an additional preprocessing tool for `SABINE`_, which predicts the DNA-motif bound by a transcription factor, given its amino acid sequence, superclass, DNA-binding domains and organism.

Availability
-------------

TFpredict is available in two different versions that are merged into one branch. During execution the ``-prokaryote`` argument can be used to choose between the classification of transcription factors from two different domains:

1. Eukaryotic transcription factors (default)
2. Prokaryotic σ-factors (``-prokaryote`` option): The algorithm itself is identical. What is different are the training data and weights for both scenarios. So, this distinction is more for convenience to directly provide preconfigured versions of TFpredict for both domains of organisms.

Introduction
-------------

Transcription factors (TF) are the key regulators of cell- and tissue-specific regulation of gene expression and play a crucial role in the orchestration of diverse biological processes, such as cell differentiation and the adaptation to changed environmental conditions. The induction or activation of target genes is achieved by the specific recognition of a DNA-motif located in the corresponding promoter regions, which is specifically recognized by the DNA-binding domain(s) of a TF. The specific interactions between TFs and their target genes are of high relevance for a more profound understanding of transcriptional gene expression in eukaryotes.

In recent work, we presented a novel method for the inference of the DNA-motif recognized by a particular TF, which is inferred from sequence-based features using Support Vector Regression. This method has been implemented in the tool `SABINE`_ (Stand-Alone BINding specificity Estimator) which is also available from our website. Besides the protein sequence, SABINE requires knowledge of the structural superclass and the DNA-binding domains of the input TF. Here, we present TFpredict, a tool which can 1) reliable distinguish TFs from other proteins, 2) predict the structural superclass of a TF and 3) detects its the DNA-binding domains. As TFpredict returns all structural information needed by SABINE to predict the DNA-motif of a given TF, we recommend the combined use of the two complementary tools.

TFpredict employs supervised machine learning methods implemented in the `WEKA`_ package for the classification of protein sequences. First, a binary classifier is used for the discrimination of TFs from other proteins (Non-TFs), and in a second step, a multi-class classifier is employed for superclass (Basic domain, Zinc Finger, Helix-turn-helix, Beta scaffold or Other) prediction. A look-up complements the second prediction step in the `TransFac TF Classification`_ in which the superclass of the input TF may already be annotated. A `BLAST+`_ search is performed to obtain the feature representation of the input sequences and the homology of a query sequence to other proteins with known class is captured using a novel feature representation called bit score percentile features. Next, the domain composition of the query sequence is reconstructed using the tool InterProScan, and the DNA-binding domains are filtered based on a pre-defined set of `GO terms`_.

**Summary:** The tool TFpredict provides an effective means for the identification and structural annotation of transcription factors (TFs), based on sequence homology features inferred from their amino acid sequence using the tool BLAST. In short, TFpredict combines sequence similarity searching with supervised machine learning methods (e.g., SVM, KNN, Naive Bayes) from the WEKA package for the identification of TFs and the prediction of their structural superclass. Furthermore, using the domain detection tool `InterProScan`_ in conjunction with a gene ontology-based filter the sequence regions spanned by DNA-binding domains are identified. If the tool is used in conjunction with SABINE the DNA motif of the TF may be determined in another prediction step. For this purpose, TFpredict generates a machine-readable text file which can be post-processed using the tool SABINE to perform the inference of the DNA-motif recognized by the given TF.



.. _`BLAST+`: https://blast.ncbi.nlm.nih.gov/doc/blast-help/downloadblastdata.html#downloadblastdata
.. _`InterProScan`: https://www.ebi.ac.uk/interpro/about/interproscan/
.. _`SABINE`: https://github.com/draeger-lab/SABINE
.. _`WEKA` : https://weka.sourceforge.io/packageMetaData/
.. _`TransFac TF Classification` : http://gene-regulation.com/pub/databases/transfac/clSM.html
.. _`GO terms` : https://www.geneontology.org/