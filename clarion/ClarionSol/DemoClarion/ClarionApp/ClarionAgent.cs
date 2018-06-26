using System;
using System.Collections.Generic;
using System.Linq;
using System.Globalization;
using Clarion;
using Clarion.Framework;
using Clarion.Framework.Core;
using Clarion.Framework.Templates;
using ClarionApp.Model;
using ClarionApp;
using System.Threading;
using Gtk;

namespace ClarionApp
{
    /// <summary>
    /// Public enum that represents all possibilities of agent actions
    /// </summary>
    public enum CreatureActions
    {
        DO_NOTHING,
        ROTATE_CLOCKWISE,
        GO_AHEAD,
		EAT_FOOD,
		GET_JEWEL,
		GO_TO_JEWEL
    }

    public class ClarionAgent
    {
        #region Constants
        /// <summary>
        /// Constant that represents the Visual Sensor
        /// </summary>
        private String SENSOR_VISUAL_DIMENSION = "VisualSensor";
        /// <summary>
        /// Constant that represents that there is at least one wall ahead
        /// </summary>
        private String DIMENSION_WALL_AHEAD = "WallAhead";
		private String DIMENSION_FOOD_AHEAD = "FoodAhead";
		private String DIMENSION_JEWEL_AHEAD = "JewelAhead";
		private String DIMENSION_NEED_JEWEL = "NeedJewel";
		double prad = 0;
        #endregion

        #region Properties
		public MindViewer mind;
		String creatureId = String.Empty;
		String creatureName = String.Empty;
		String jewelFound = String.Empty;
		String foodFound = String.Empty;
		double x_go = 100.0;
		double y_go = 100.0;

        #region Simulation
        /// <summary>
        /// If this value is greater than zero, the agent will have a finite number of cognitive cycle. Otherwise, it will have infinite cycles.
        /// </summary>
        public double MaxNumberOfCognitiveCycles = -1;
        /// <summary>
        /// Current cognitive cycle number
        /// </summary>
        private double CurrentCognitiveCycle = 0;
        /// <summary>
        /// Time between cognitive cycle in miliseconds
        /// </summary>
        public Int32 TimeBetweenCognitiveCycles = 0;
        /// <summary>
        /// A thread Class that will handle the simulation process
        /// </summary>
        private Thread runThread;
        #endregion

        #region Agent
		private WSProxy worldServer;
        /// <summary>
        /// The agent 
        /// </summary>
        private Clarion.Framework.Agent CurrentAgent;
        #endregion

        #region Perception Input
        /// <summary>
        /// Perception input to indicates a wall ahead
        /// </summary>
		private DimensionValuePair inputWallAhead;
		private DimensionValuePair inputFoodAhead;
		private DimensionValuePair inputJewelAhead;
		private DimensionValuePair inputNeedJewel;

        #endregion

        #region Action Output
        /// <summary>
        /// Output action that makes the agent to rotate clockwise
        /// </summary>
		private ExternalActionChunk outputRotateClockwise;
        /// <summary>
        /// Output action that makes the agent go ahead
        /// </summary>
		private ExternalActionChunk outputGoAhead;

		private ExternalActionChunk outputGetJewel;

		private ExternalActionChunk outputEatFood;

		private ExternalActionChunk outputGoToJewel;
        
		#endregion

        #endregion

        #region Constructor
		public ClarionAgent(WSProxy nws, String creature_ID, String creature_Name)
        {
			worldServer = nws;
			// Initialize the agent
            CurrentAgent = World.NewAgent("Current Agent");
			mind = new MindViewer();
			mind.Show();
			creatureId = creature_ID;
			creatureName = creature_Name;

            // Initialize Input Information
            inputWallAhead = World.NewDimensionValuePair(SENSOR_VISUAL_DIMENSION, DIMENSION_WALL_AHEAD);
			inputFoodAhead = World.NewDimensionValuePair(SENSOR_VISUAL_DIMENSION, DIMENSION_FOOD_AHEAD);
			inputJewelAhead = World.NewDimensionValuePair(SENSOR_VISUAL_DIMENSION, DIMENSION_JEWEL_AHEAD);
			inputNeedJewel = World.NewDimensionValuePair(SENSOR_VISUAL_DIMENSION, DIMENSION_NEED_JEWEL);


            // Initialize Output actions
            outputRotateClockwise = World.NewExternalActionChunk(CreatureActions.ROTATE_CLOCKWISE.ToString());
            outputGoAhead = World.NewExternalActionChunk(CreatureActions.GO_AHEAD.ToString());
			outputGetJewel = World.NewExternalActionChunk(CreatureActions.GET_JEWEL.ToString());
			outputEatFood = World.NewExternalActionChunk(CreatureActions.EAT_FOOD.ToString());
			outputGoToJewel = World.NewExternalActionChunk(CreatureActions.GO_TO_JEWEL.ToString());

            //Create thread to simulation
            runThread = new Thread(CognitiveCycle);
			Console.WriteLine("Agent started");
        }
        #endregion

        #region Public Methods
        /// <summary>
        /// Run the Simulation in World Server 3d Environment
        /// </summary>
        public void Run()
        {                
			Console.WriteLine ("Running ...");
            // Setup Agent to run
            if (runThread != null && !runThread.IsAlive)
            {
                SetupAgentInfraStructure();
				// Start Simulation Thread                
                runThread.Start(null);
            }
        }

        /// <summary>
        /// Abort the current Simulation
        /// </summary>
        /// <param name="deleteAgent">If true beyond abort the current simulation it will die the agent.</param>
        public void Abort(Boolean deleteAgent)
        {
			Console.WriteLine ("Aborting ...");
            if (runThread != null && runThread.IsAlive)
            {
                runThread.Abort();
            }

            if (CurrentAgent != null && deleteAgent)
            {
                CurrentAgent.Die();
            }
        }

		IList<Thing> processSensoryInformation()
		{
			IList<Thing> response = null;

			if (worldServer != null && worldServer.IsConnected)
			{
				response = worldServer.SendGetCreatureState(creatureName);
				prad = (Math.PI / 180) * response.First().Pitch;
				while (prad > Math.PI) prad -= 2 * Math.PI;
				while (prad < - Math.PI) prad += 2 * Math.PI;
				Sack s = worldServer.SendGetSack("0");
				mind.setBag(s);
			}

			return response;
		}

		void processSelectedAction(CreatureActions externalAction)
		{
			Thread.CurrentThread.CurrentCulture = new CultureInfo("en-US");
			if (worldServer != null && worldServer.IsConnected)
			{
				switch (externalAction)
				{
				case CreatureActions.DO_NOTHING:
					// Do nothing as the own value says
					break;
				case CreatureActions.ROTATE_CLOCKWISE:
					worldServer.SendSetAngle(creatureId, 1, -1, 2);
					break;
				case CreatureActions.GO_AHEAD:
					//worldServer.SendSetAngle(creatureId, 1, 1, prad);
					worldServer.SendSetAngle(creatureId, 1, -1, 2);
					break;
				case CreatureActions.EAT_FOOD:
					// CHANGE
					if(foodFound != String.Empty)
						worldServer.SendEatIt (creatureId, foodFound);
					else
						worldServer.SendSetAngle(creatureId, 1, -1, 2);
					break;
				case CreatureActions.GET_JEWEL:
					if(jewelFound != String.Empty)
						worldServer.SendSackIt (creatureId, jewelFound);
					else
						worldServer.SendSetAngle(creatureId, 1, -1, 2);
					break;
				case CreatureActions.GO_TO_JEWEL:
					worldServer.SendSetGoTo(creatureId, 1, 1, x_go, y_go);
					//worldServer.SendSetAngle(creatureId, 2, -2, 2);
					break;
				default:
					break;
				}
			}
		}

        #endregion

        #region Setup Agent Methods
        /// <summary>
        /// Setup agent infra structure (ACS, NACS, MS and MCS)
        /// </summary>
        private void SetupAgentInfraStructure()
        {
            // Setup the ACS Subsystem
            SetupACS();                    
        }

        private void SetupMS()
        {            
            //RichDrive
        }

        /// <summary>
        /// Setup the ACS subsystem
        /// </summary>
        private void SetupACS()
        {
            // Create Rule to avoid collision with wall
            SupportCalculator avoidCollisionWallSupportCalculator = FixedRuleToAvoidCollisionWall;
            FixedRule ruleAvoidCollisionWall = AgentInitializer.InitializeActionRule(CurrentAgent, FixedRule.Factory, outputRotateClockwise, avoidCollisionWallSupportCalculator);
			// Commit this rule to Agent (in the ACS)
			CurrentAgent.Commit(ruleAvoidCollisionWall);

			// Create Rule to eat food
			SupportCalculator eatFoodSupportCalculator = FixedRuleToEatFood;
			FixedRule ruleEatFood= AgentInitializer.InitializeActionRule(CurrentAgent, FixedRule.Factory, outputEatFood, eatFoodSupportCalculator);
			// Commit this rule to Agent (in the ACS)
			CurrentAgent.Commit(ruleEatFood);

			// Create Rule to get jewel
			SupportCalculator getJewelSupportCalculator = FixedRuleToGetJewel;
			FixedRule ruleGetJewel = AgentInitializer.InitializeActionRule(CurrentAgent, FixedRule.Factory, outputGetJewel, getJewelSupportCalculator);
			// Commit this rule to Agent (in the ACS)
			CurrentAgent.Commit(ruleGetJewel);

			// Create Colission To Go To Jewel
			SupportCalculator goToJewelSupportCalculator = FixedRuleToGoToJewel;
			FixedRule ruleGoToJewel = AgentInitializer.InitializeActionRule(CurrentAgent, FixedRule.Factory, outputGoToJewel, goToJewelSupportCalculator);
			// Commit this rule to Agent (in the ACS)
			CurrentAgent.Commit(ruleGoToJewel);

            // Create Colission To Go Ahead
            SupportCalculator goAheadSupportCalculator = FixedRuleToGoAhead;
            FixedRule ruleGoAhead = AgentInitializer.InitializeActionRule(CurrentAgent, FixedRule.Factory, outputGoAhead, goAheadSupportCalculator);
            // Commit this rule to Agent (in the ACS)
            CurrentAgent.Commit(ruleGoAhead);

            // Disable Rule Refinement
            CurrentAgent.ACS.Parameters.PERFORM_RER_REFINEMENT = false;

            // The selection type will be probabilistic
            CurrentAgent.ACS.Parameters.LEVEL_SELECTION_METHOD = ActionCenteredSubsystem.LevelSelectionMethods.STOCHASTIC;

            // The action selection will be fixed (not variable) i.e. only the statement defined above.
            CurrentAgent.ACS.Parameters.LEVEL_SELECTION_OPTION = ActionCenteredSubsystem.LevelSelectionOptions.FIXED;

            // Define Probabilistic values
            CurrentAgent.ACS.Parameters.FIXED_FR_LEVEL_SELECTION_MEASURE = 1;
            CurrentAgent.ACS.Parameters.FIXED_IRL_LEVEL_SELECTION_MEASURE = 0;
            CurrentAgent.ACS.Parameters.FIXED_BL_LEVEL_SELECTION_MEASURE = 0;
            CurrentAgent.ACS.Parameters.FIXED_RER_LEVEL_SELECTION_MEASURE = 0;
        }

        /// <summary>
        /// Make the agent perception. In other words, translate the information that came from sensors to a new type that the agent can understand
        /// </summary>
        /// <param name="sensorialInformation">The information that came from server</param>
        /// <returns>The perceived information</returns>
		private SensoryInformation prepareSensoryInformation(IList<Thing> listOfThings)
		{
			// New sensory information
			SensoryInformation si = World.NewSensoryInformation (CurrentAgent);

			// Detect if we have a wall ahead
			Boolean wallAhead = listOfThings.Where (item => (item.CategoryId == Thing.CATEGORY_BRICK && item.DistanceToCreature <= 21)).Any ();
			// double wallAheadActivationValue = wallAhead ? CurrentAgent.Parameters.MAX_ACTIVATION : CurrentAgent.Parameters.MIN_ACTIVATION;
			double wallAheadActivationValue = wallAhead ? CurrentAgent.Parameters.MAX_ACTIVATION : CurrentAgent.Parameters.MIN_ACTIVATION;
			si.Add (inputWallAhead, wallAheadActivationValue);

			// Detect if we have food close by
			IEnumerable<Thing> foods = listOfThings.Where (item => ((item.CategoryId == Thing.CATEGORY_NPFOOD || item.CategoryId == Thing.CATEGORY_PFOOD) && item.DistanceToCreature <= 61));
			double foodAheadActivationValue;
			if (foods.Any ()) {
				foodFound = foods.First ().Name;
				foodAheadActivationValue = CurrentAgent.Parameters.MAX_ACTIVATION;
			} else {
				foodFound = String.Empty;
				foodAheadActivationValue = CurrentAgent.Parameters.MIN_ACTIVATION;
			}
			si.Add(inputFoodAhead, foodAheadActivationValue);


			// Detect if we have jewel close by
			IEnumerable<Thing> closeJewels = listOfThings.Where(item => (item.CategoryId == Thing.CATEGORY_JEWEL && item.DistanceToCreature <= 61));
			double jewelAheadActivationValue;
			if (closeJewels.Any()) {
				jewelFound = closeJewels.First ().Name;
				jewelAheadActivationValue = CurrentAgent.Parameters.MAX_ACTIVATION;
			} else {
				jewelFound = String.Empty;
				jewelAheadActivationValue = CurrentAgent.Parameters.MIN_ACTIVATION;
			}
			si.Add(inputJewelAhead, jewelAheadActivationValue);



			//Console.WriteLine(sensorialInformation);
			Creature c = (Creature) listOfThings.Where(item => (item.CategoryId == Thing.CATEGORY_CREATURE)).First();
			int n = 0;
			foreach(Leaflet l in c.getLeaflets()) {
				mind.updateLeaflet(n,l);
				n++;
			}
			mind.updateTotLeaflet ();

			// Detect if has a required leaflet and prepare its position
			IEnumerable<Thing> jewels = listOfThings.Where(item => false);
			IEnumerable<Thing> redJewels = listOfThings.Where(item => (item.CategoryId == Thing.CATEGORY_JEWEL && item.Material.Color == "Red"));
			double goToJewelActivationValue;
			if (redJewels.Any () && ((mind.totLeaflet [0] - mind.red) > 0)) {
				jewels = jewels.Concat(redJewels);
			}
			IEnumerable<Thing> greenJewels = listOfThings.Where(item => (item.CategoryId == Thing.CATEGORY_JEWEL && item.Material.Color == "Green"));
			if (greenJewels.Any () && ((mind.totLeaflet [1] - mind.green) > 0)) {
				jewels = jewels.Concat (greenJewels);
			}
			IEnumerable<Thing> blueJewels = listOfThings.Where(item => (item.CategoryId == Thing.CATEGORY_JEWEL && item.Material.Color == "Blue"));
			if (blueJewels.Any () && ((mind.totLeaflet [2] - mind.blue) > 0)) {
				jewels = jewels.Concat (blueJewels);
			}
			IEnumerable<Thing> yellowJewels = listOfThings.Where(item => (item.CategoryId == Thing.CATEGORY_JEWEL && item.Material.Color == "Yellow"));
			if (yellowJewels.Any () && ((mind.totLeaflet [3] - mind.yellow) > 0)) {
				jewels = jewels.Concat (yellowJewels);
			}
			IEnumerable<Thing> magentaJewels = listOfThings.Where(item => (item.CategoryId == Thing.CATEGORY_JEWEL && item.Material.Color == "Magenta"));
			if (magentaJewels.Any () && ((mind.totLeaflet[4]-mind.magenta) > 0)) {
				jewels = jewels.Concat (magentaJewels);
			}
			IEnumerable<Thing> whiteJewels = listOfThings.Where(item => (item.CategoryId == Thing.CATEGORY_JEWEL && item.Material.Color == "White"));
			if (whiteJewels.Any () && ((mind.totLeaflet[5]-mind.white) > 0)) {
				jewels = jewels.Concat (whiteJewels);
			}

			if (jewels.Any ()) {
				jewels = jewels.OrderBy (item => item.DistanceToCreature);
				x_go = jewels.First ().X1;
				y_go = jewels.First ().Y1;
				goToJewelActivationValue = CurrentAgent.Parameters.MAX_ACTIVATION;
			} else {
				goToJewelActivationValue = CurrentAgent.Parameters.MIN_ACTIVATION;
			}
			si.Add(inputNeedJewel, goToJewelActivationValue);

            return si;
        }
        #endregion

        #region Fixed Rules
        private double FixedRuleToAvoidCollisionWall(ActivationCollection currentInput, Rule target)
        {
            // See partial match threshold to verify what are the rules available for action selection
            return ((currentInput.Contains(inputWallAhead, CurrentAgent.Parameters.MAX_ACTIVATION))) ? 1.0 : 0.0;
        }

		private double FixedRuleToEatFood(ActivationCollection currentInput, Rule target)
		{
			// See partial match threshold to verify what are the rules available for action selection
			return ((currentInput.Contains(inputFoodAhead, CurrentAgent.Parameters.MAX_ACTIVATION))) ? 1.0 : 0.0;
		}

		private double FixedRuleToGetJewel(ActivationCollection currentInput, Rule target)
		{
			// See partial match threshold to verify what are the rules available for action selection
			return ((currentInput.Contains(inputJewelAhead, CurrentAgent.Parameters.MAX_ACTIVATION))) ? 1.0 : 0.0;
		}

		private double FixedRuleToGoToJewel(ActivationCollection currentInput, Rule target)
		{
			// Here we will make the logic to go ahead
			return ((currentInput.Contains(inputNeedJewel, CurrentAgent.Parameters.MAX_ACTIVATION))) ? 1.0 : 0.0;
		}

        private double FixedRuleToGoAhead(ActivationCollection currentInput, Rule target)
        {
            // Here we will make the logic to go ahead
			return ((currentInput.Contains(inputNeedJewel, CurrentAgent.Parameters.MIN_ACTIVATION))) ? 1.0 : 0.0;
        }
        #endregion

        #region Run Thread Method
        private void CognitiveCycle(object obj)
        {

			Console.WriteLine("Starting Cognitive Cycle ... press CTRL-C to finish !");
            // Cognitive Cycle starts here getting sensorial information
            while (CurrentCognitiveCycle != MaxNumberOfCognitiveCycles)
            {   
				// Get current sensory information                    
				IList<Thing> currentSceneInWS3D = processSensoryInformation();

                // Make the perception
                SensoryInformation si = prepareSensoryInformation(currentSceneInWS3D);

                //Perceive the sensory information
                CurrentAgent.Perceive(si);

                //Choose an action
                ExternalActionChunk chosen = CurrentAgent.GetChosenExternalAction(si);


                // Get the selected action
                String actionLabel = chosen.LabelAsIComparable.ToString();
                CreatureActions actionType = (CreatureActions)Enum.Parse(typeof(CreatureActions), actionLabel, true);

                // Call the output event handler
				processSelectedAction(actionType);

                // Increment the number of cognitive cycles
                CurrentCognitiveCycle++;

                //Wait to the agent accomplish his job
                if (TimeBetweenCognitiveCycles > 0)
                {
                    Thread.Sleep(TimeBetweenCognitiveCycles);
                }
			}
        }
        #endregion

    }
}
