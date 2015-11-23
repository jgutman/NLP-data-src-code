#!/usr/bin/env python

"""experiments.py: Run experiments with word2vec in gensim module and save word embeddings to file."""

__author__      = "Jacqueline Gutman"
__email__ = "jacqueline.gutman@nyu.edu"

# import modules
import gensim, logging
import argparse, os

# borrowed from http://rare-technologies.com/word2vec-tutorial
class MySentences(object):
	def __init__(self, dirname, nfiles):
		self.dirname = dirname
		self.nfiles = nfiles
		
	def __iter__(self):
		if (self.nfiles == 0):
			for dataFile in os.listdir(self.dirname):
				for sentence in open(os.path.join(self.dirname, dataFile)):
					yield sentence.split()
		else:
			files = os.listdir(self.dirname)[0:self.nfiles]
			for dataFile in files:
				for sentence in open(os.path.join(self.dirname, dataFile)):
					yield sentence.split()
				
def analyze(model):
	questionFile = 'questions-words.txt'
	questionFile = os.path.abspath('../data5/'+questionFile)
	model.accuracy(questionFile)

def main():
	# set up logging : borrowed from http://rare-technologies.com/word2vec-tutorial/
	logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level = logging.INFO)

	# set up argument parsing from command line
	parser = argparse.ArgumentParser(description='Get training data path')
	parser.add_argument('dataPath', help='location of training sentences')
	parser.add_argument('-embeddings', dest='embed', help='output file for storing embeddings')
	parser.add_argument('-directory', dest='data_dir', help='is data path a directory of data files?',
		action='store_true')
	parser.add_argument('-load', dest='load_model', help='load previously built model from binary',
		action='store_true')
	parser.add_argument('-window', dest='context_size', help='size of context window for the model',
		type=int)
	parser.add_argument('-dimension', dest='dimensions', help='dimensionality of word embeddings',
		type=int)
	parser.add_argument('-negative', dest='k_negative_samples', help='number of negatively sampled contexts to use',
		type=int)
	parser.add_argument('-nfiles', dest='nDataFiles', help='number of training data files to read in',
		type=int)
	parser.set_defaults(data_dir = False, load_model = False, 
		context_size = 5, dimensions = 100, k_negative_samples=0, nDataFiles=0)
	args = parser.parse_args()

	# read in training sentence data
	if (args.data_dir):
		sentences = MySentences(args.dataPath, args.nDataFiles)
		#sentences = gensim.models.word2vec.LineSentence(args.dataPath)
		
	else:
		dataFile = open(args.dataPath, "r")
		sentencesWhole = dataFile.readlines()
		dataFile.close()
		sentences = [sentence.split() for sentence in sentencesWhole]
	
	# initialize a word2vec model from the training sentences
	if (args.load_model):
		model = gensim.models.Word2Vec.load(os.path.abspath(args.embed+'.bin'))
	
	else:
		#if (args.data_dir):
		#	bigram = gensim.models.Phrases()
		#else:
		#	bigram = gensim.models.Phrases(sentences)
			
		model = gensim.models.Word2Vec(sentences, size=args.dimensions, window=args.context_size, 
			negative=args.k_negative_samples, min_count=5, workers=4)
		#model = gensim.models.Word2Vec(bigram[sentences], size=args.dimensions, window=args.context_size, 
		#	negative=args.k_negative_samples, min_count=5, workers=4)
		model.save_word2vec_format(args.embed, binary=False)
		model.save(os.path.abspath(args.embed+'.bin'))
	
	analyze(model)

if __name__ == "__main__":
	main()