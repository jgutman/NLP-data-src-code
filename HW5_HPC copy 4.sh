#!/bin/bash

#PBS -l mem=16GB
#PBS -l walltime=6:00:00
#PBS -l nodes=1:ppn=1:ivybridge
#PBS -M jg3862@nyu.edu
#PBS -m ae

module load python/intel/2.7.6
module load jdk/1.7.0_60
module load gensim/intel/0.10.3 
module load nltk/3.0.2

RUNDIR=$SCRATCH/src-3/HW5.run.$PBS_JOBID
mkdir -p $RUNDIR
cd $RUNDIR/..

python experiments.py ../data5/training-data/1-billion-word-language-modeling-benchmark-r13output/training-monolingual.tokenized.shuffled/ -directory -nfiles 10 -window 10 -dimension 200 -negative 10 -embeddings ../data5/embeddings/billion_C10_N200_K10_F10.txt

java -cp classes nlp.assignments.WordSimTester -embeddings ../data5/embeddings/billion_C10_N200_K10_F10.txt -wordsim ../data5/wordsim353/combined.csv