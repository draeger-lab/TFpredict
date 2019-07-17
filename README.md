TFpredict
=========
<img align="right" src="doc/tfpredict_logo.png" title="TFpredict"/> 

**Identification and structural characterization of transcription factors based on supervised machine learning**

[![License (GPL version 3)](https://img.shields.io/badge/license-GPLv3.0-blue.svg?style=plastic)](http://opensource.org/licenses/GPL-3.0)
[![Stable version](https://img.shields.io/badge/Stable_version-1.3-brightgreen.svg?style=plastic)](https://github.com/draeger-lab/TFpredict/releases/)
[![DOI](http://img.shields.io/badge/DOI-10.1371%20%2F%20journal.pone.0082238-blue.svg?style=plastic)](http://dx.doi.org/10.1371/journal.pone.0082238)

*Authors:* [Johannes Eichner](https://github.com/jeichner/)＊, [Florian Topf](https://github.com/ftopf)＊, [Andreas Dräger](https://github.com/draeger/), [James T. Yurkovich](https://github.com/jtyurkovich/), [Michael Römer](https://github.com/mroemer/)

＊These two authors contributed equally to this work.
___________________________________________________________________________________________________________


Article citations are **critical** for us to be able to continue support for TFpredict.  If you use TFpredict and you publish papers about work that uses TFpredict, we ask that you **please cite the TFpredict paper**.

<dl>
  <dt>Research Article:</dt>
  <dd>Johannes Eichner, Florian Topf, Andreas Dräger, Clemens Wrzodek, Dierk Wanke, and Andreas Zell. <a href="http://dx.doi.org/10.1371%2Fjournal.pone.0082238">TFpredict and SABINE: Sequence-Based Prediction of Structural and Functional Characteristics of Transcription Factors</a>. PLoS ONE, 8(12):e82238, December 2013.
  [ <a href="http://dx.doi.org/10.1371/journal.pone.0082238">DOI</a> | <a href="http://www.plosone.org/article/fetchObject.action?uri=info%3Adoi%2F10.1371%2Fjournal.pone.0082238&representation=PDF">PDF</a> ]  
  </dd>
</dl>

___________________________________________________________________________________________________________

**TFpredict** is a tool which implements a novel three-step classification method which expects a protein sequence as input and (1) distinguishes transcription factors (TF) from other proteins (Non-TF), (2) predicts the structural superclass of TFs (see TransFac classification), and (3) identifies the DNA-binding domains of TFs. The latter two classification steps are only to be performed if the given protein sequence was identified as a TF. The tool incorporates the results from a [BLAST+](http://blast.ncbi.nlm.nih.gov/Blast.cgi?CMD=Web&PAGE_TYPE=BlastDocs&DOC_TYPE=Download) search into a novel feature representation which allows TF/non-TF classification by state-of-the-art machine learning methods. Specific supervised classifiers were constructed for the task of identifying TFs and their structural superclasses, respectively. Next, known protein domains are detected by the tool [InterProScan](http://www.ebi.ac.uk/Tools/pfa/iprscan/), and then the DNA-binding domains among these are filtered through GO-terms. TFpredict was implemented as an additional preprocessing tool for [SABINE](https://github.com/draeger-lab/SABINE), which predicts the DNA-motif bound by a transcription factor, given its amino acid sequence, superclass, DNA-binding domains and organism.
___________________________________________________________________________________________________________
  
Availability
------------

Please note that TFpredict is available in **two different versions** that are currently organized in two separate branches. Each branch can be used for classification of transcription factors from a different domain:
1. Eukaryotic transcription factors ([master branch](https://github.com/draeger-lab/TFpredict/tree/master))
2. Prokaryotic σ-factors ([prokaryote branch](https://github.com/draeger-lab/TFpredict/tree/prokaryote))
The algorithm itself is identical. What is different are the training data and weights for both scenarios. So, this distinctionis more for convenience to directly provide preconfigured versions of TFpredict for both domains of organisms.

Table of Contents
-----------------

 * [Introduction](#Introduction)
 * [How to get started](#How-to-get-started)
 * [Installation](#Installation)
 * [Manual](#Manual)
 * [Format specification](#Format-specification)
 * [Copyright and license](#Copyright-and_License)
 * [Acknowledgments](#Acknowledgments)
 * [Contact](#Contact)

Introduction
------------

Transcription factors (TF) are the key regulators of cell- and tissue-specific regulation of gene expression and play a crucial role in the orchestration of diverse biological processes, such as cell differentiation and the adaptation to changed environmental conditions. The induction or activation of target genes is achieved by the specific recognition of a DNA-motif located in the corresponding promoter regions, which is specifically recognized by the DNA-binding domain(s) of a TF. The specific interactions between TFs and their target genes are of high relevance for a more profound understanding of transcriptional gene expression in eukaryotes. 

In recent work, we presented a novel method for the inference of the DNA-motif recognized by a particular TF, which is inferred from sequence-based features using Support Vector Regression. This method has been implemented in the tool SABINE (Stand-Alone BINding specificity Estimator) which is also available from our website. Besides the protein sequence, [SABINE](https://github.com/draeger-lab/SABINE/) requires knowledge of the structural superclass and the DNA-binding domains of the input TF. Here, we present TFpredict, a tool which can 1) reliable distinguish TFs from other proteins, 2) predict the structural superclass of a TF and 3) detects its the DNA-binding domains. As TFpredict returns all structural information needed by SABINE to predict the DNA-motif of a given TF, we recommend the combined use of the two complementary tools. 

TFpredict employs supervised machine learning methods implemented in the [WEKA](http://www.cs.waikato.ac.nz/~ml/weka/index.html) package for the classification of protein sequences. First, a binary classifier is used for the discrimination of TFs from other proteins (Non-TFs), and in a second step, a multi-class classifier is employed for superclass (Basic domain, Zinc Finger, Helix-turn-helix, Beta scaffold or Other) prediction. A look-up complements the second prediction step in the [TransFac TF Classification](http://www.gene-regulation.com/pub/databases/transfac/clSM.html) in which the superclass of the input TF may already be annotated. A [BLAST+](http://blast.ncbi.nlm.nih.gov/Blast.cgi?CMD=Web&PAGE_TYPE=BlastDocs&DOC_TYPE=Download) search is performed to obtain the feature representation of the input sequences and the homology of a query sequence to other proteins with known class is captured using a novel feature representation called bit score percentile features. Next, the domain composition of the query sequence is reconstructed using the tool [InterProScan](http://www.ebi.ac.uk/Tools/pfa/iprscan/), and the DNA-binding domains are filtered based on a pre-defined set of [GO terms](http://www.geneontology.org).

**Summary:** The tool TFpredict provides an effective means for the identification and structural annotation of transcription factors (TFs), based on sequence homology features inferred from their amino acid sequence using the tool [BLAST](http://blast.ncbi.nlm.nih.gov/Blast.cgi?CMD=Web&PAGE_TYPE=BlastDocs&DOC_TYPE=Download). In short, TFpredict combines sequence similarity searching with supervised machine learning methods (e.g., SVM, KNN, Naive Bayes) from the [WEKA package](http://www.cs.waikato.ac.nz/~ml/weka/index.html) for the identification of TFs and the prediction of their structural superclass. Furthermore, using the domain detection tool [InterProScan](http://www.ebi.ac.uk/Tools/pfa/iprscan/) in conjunction with a gene ontology-based filter the sequence regions spanned by DNA-binding domains are identified. If the tool is used in conjunction with [SABINE](https://github.com/draeger-lab/SABINE/) the DNA motif of the TF may be determined in another prediction step. For this purpose, TFpredict generates a machine-readable text file which can be post-processed using the tool SABINE to perform the inference of the DNA-motif recognized by the given TF.


How to get started
------------------

  The stand-alone version of TFpredict is equipped with a command-line interface which can be used for the batch processing of multiple protein sequences given in FASTA format. For convenience, TFpredict uses the webservice version of InterproScan. Thus, installing the perl stand-alone version of InterProScan (approx. 40GB) is not required. To support applications, which require the processing of a large number of sequences (e.g., the genome-wide prediction of TFs in a specific organism) TFpredict can alternatively be used with a local installation of InterProScan.

Installation
------------

  Download the JAR file TFPredict from https://github.com/draeger-lab/TFpredict/releases and also the example file [test_seq.fasta](test_seq.fasta).
  
  You can also clone this repository and build a new snapshot release using the `ant` script shipped with this project by executing the following command:
  
    ant jar_incl-lib

  TFpredict is completely implemented in Java and provided as a runnable JAR file. All platforms (Windows, Mac, Linux) 
  are supported provided that Java (JDK 1.6 or later) and BLAST (NCBI BLAST 2.2.27+ or later) is installed. 
  You can download the latest version of BLAST from ftp://ftp.ncbi.nlm.nih.gov/blast/executables/blast+/LATEST/.
  
  Requirements:
  * [Java&trade;](https://www.java.com) (JDK 1.6 or later)
  + BLAST (NCBI BLAST 2.2.27+ or later)
  
  The analysis framework of TFpredict is entirely written in Java. Thus, it requires that Java Virtual Machine (JDK version 1.6 or newer) is installed on your system.
  
### Installation and configuration of BLAST
  
Follow the instructions at the BLAST home-page for your operating system. For Unix, go to http://www.ncbi.nlm.nih.gov/books/NBK52640/
For Windows, see the instructions http://www.ncbi.nlm.nih.gov/books/NBK52637/.
   
Having BLAST successfully installed, it is necessary to pass the path to the executable to TFpredict. To this end, define the environment variable
   
    BLAST_DIR
   
on your system to point to the installation directory of your copy of BLAST. This might be, for instance,
   
|Path|Operating System|
|--|--|
| `/usr/local/ncbi/blast/` | macOS |
| `/opt/blast/latest/`     | Linux |
| `C:\program~\NCBI\blast-x.x.xxx\` | MS Windows |
   
Note that `x.x.xxx` stands for some arbitrary version number of BLAST and must be replaced as necessary.

So, the variable `BLAST_DIR` could be set to one of the above example folders. In the bash under MacOS you would, e.g., type
   
    export BLAST_DIR=/usr/local/ncbi/blast/
   
before executing TFpredict.

_____________________________________________________________________________________________________________


  Manual
  ------

  **INPUT:**  FASTA file: contains the protein identifiers and sequences in FASTA format  (see Format Specification). You can find the example file [test_seq.fasta](test_seq.fasta) in the project's main folder.
  
  **OUTPUT:** SABINE input file: contains all information required for post-processing the results with SABINE (see Format Specification at https://github.com/draeger-lab/SABINE/). The output filename can be specified by the user (see OPTIONS: `-sabineOutfile`). The argument `-species` also has to be specified if an output file for SABINE shall be created.
  
  **USAGE:**
  
  ```
  java -jar TFpredict.jar <input_filename> [OPTIONS]
  ```
  
  **OPTIONS:**
  * `-sabineOutfile <output_filename>` Output file for post-processing of the results with SABINE.
  * `-species <organism_name>` Organism name (e.g., Homo sapiens). See list of supported organisms: http://www.cogsys.cs.uni-tuebingen.de/software/SABINE/doc/organism_list.txt
  * `-tfClassifier <classifier_name>` Classifier used for TF/non-TF classification possible values: SVM_linear, NaiveBayes, KNN
  * `-superClassifier <classifier_name>` Classifier used for superclass prediction possible values: SVM_linear, NaiveBayes, KNN
  * `-iprscanPath <path_to_iprscan>` Path to `iprscan` executable from local InterProScan installation. Only needed if you have a local installation of InterProScan which shall be used by TFpredict.
  * `-blastPath <path_to_blast>` Path to "bin" directory containing BLAST executables (e.g., `/opt/blast/latest`). Only needed if environment variable BLAST_PATH is not set.
  * `-ignoreCharacteristicDomains` no classification based on predefined InterPro domains.
  * `--help` to display the usage of the script and an overview of the command line options.
  
  ### How to proceed
  First, you need to generate an input file in FASTA format (see [format specification](#-Format-specification) below) or [example input file](test_seq.fasta). The input file should contain the following information about the protein under study:
* Name or identifier
* Organism (see [list of supported organisms](src/resources/organism_list.txt))
* Protein sequence

To run TFpredict on the example input file, use the command:

    java -jar TFpredict.jar test_seq.fasta
    
To post-process the results generated by TFpredict with SABINE to predict DNA-motives for transcription factors identified among the input protein sequences, you have to pass two additional arguments to the program. First, the destination to which the output file shall be written has to be specified, and second, the correct species has to be provided. Please ensure that SABINE supports the given species (see [list of supported organisms](src/resources/organism_list.txt)).

An exemplary call of the program which facilitates the post-processing of the results using the tool SABINE is shown here:

    java -jar TFpredict.jar example.input -sabineOutfile example.output -species "Homo sapiens"
    
TFpredict returns an output file, which contains the results of the performed prediction steps in the SABINE input file format.

If you have a local installation of the tool InterProScan, which shall be used by TFpredict, you have to pass the destination of the main executable of InterProScan as an argument to TFpredict.

Assuming that InterProScan was installed to the directory `/opt/iprscan` you could use the following command:

    java -jar TFpredict.jar example.input -iprscanPath /opt/iprscan/bin/iprscan
    

  ___________________________________________________________________________________________________________

  Format specification
  --------------------

To analyze a given protein with TFpredict the tool needs the corresponding amino acid sequence and organism. This information has to be formatted as specified in the TFpredict input file format description. 

The results of TFpredict are returned to the user via the standard output. Optionally, an output file can be generated which can be processed using SABINE to predict the DNA-binding specificity of transcription factors identified among the protein sequences analyzed by TFpredict. See the [SABINE input file format](https://github.com/draeger-lab/SABINE/) specification for a detailed description of the file format.

The input file format description specifies the input data for an individual TF. You can pack multiple TFs in one input file to sequentially process more extensive datasets with SABINE. In addition to the general description of the file formats, example input and output files for SABINE are provided.

  FASTA file:
  
```
  >Sequence_1
  MEEPQSDPSVEPPLSQETFSDLWKLLPENNVLSPLPSQAMDDLMLSPDDIEQWFTEDPGP
  DEAPRMPEAAPPVAPAPAAPTPAAPAPAPSWPLSSSVPSQKTYQGSYGFRLGFLHSGTAK
  SVTCTYSPALNKMFCQLAKTCPVQLWVDSTPPPGTRVRAMAIYKQSQHMTEVVRRCPHHE
  RCSDSDGLAPPQHLIRVEGNLRVEYLDDRNTFRHSVVVPYEPPEVGSDCTTIHYNYMCNS
  SCMGGMNRRPILTIITLEDSSGNLLGRNSFEVRVCACPGRDRRTEEENLRKKGEPHHELP
  PGSTKRALPNNTSSSPQPKKKPLDGEYFTLQIRGRERFEMFRELNEALELKDAQAGKEPG
  GSRAHSSHLKSKKGQSTSRHKKLMFKTEGPDSD

  >Sequence_2
  ...

  >Sequence_3
  ...
```
  
  SABINE input file:
  --------------------

```
  NA  Identifier
  XX
  SP  Organism
  XX
  CL  Classification (decimal classification no. as in TRANSFAC)
  XX
  S1  Amino acid sequence
  XX
  FT  DNA-binding domain (InterPro ID   start position   end position)
  XX
  //
  XX
```

  Copyright and License
  ---------------------

  Copyright © 2013-2018 by the individual authors of this software.

  <img align="right" src="https://www.gnu.org/graphics/gplv3-88x31.png" title="GPL Version 3" alt="GPL Version 3"/>
  This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 
  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 
  You should have received a copy of the GNU General Public License along with this program; if not, see http://www.gnu.org/licenses/.

  _____________________________________________________________________________________________________________

  Acknowledgments
  ---------------
  
  This project is promoted by

![Federal Ministry of Education and Research](https://www.bmbf.de/site/img/logo.png) ![The Virtual Liver Network](doc/VLN_left_small.png) ![Spher4Sys](doc/spher4sys_logo_small.png) ![Marie Skłowdowska-Curie Actions](doc/mca.jpg)

  Contact
  -------

  In case of any questions, please contact <a href="mailto:andreas.draeger@uni-tuebingen.de?subject=TFpredict">Andreas Dräger</a>.
