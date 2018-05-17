using System;


namespace ClarionApp
{
	public class RandomGenerator
	{
		Random randomGen;
		public RandomGenerator ()
		{
			randomGen = new Random();
			// Random randomGen = new Random(DateTime.Now.Millisecond);
		}

		public double randomDouble(double min, double max){
			return randomGen.NextDouble()*(max - min) + min;
		}

		public int randomInt(int min, int max){
			return randomGen.Next(min, max);
		}
	}
}

