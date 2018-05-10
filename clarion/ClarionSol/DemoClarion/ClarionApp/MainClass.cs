
using System;
using System.Collections;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Threading;
using ClarionApp;
using ClarionApp.Model;
using ClarionApp.Exceptions;
using Gtk;

namespace ClarionApp
{
	class MainClass
	{
		#region properties
		private WSProxy ws = null;
        private ClarionAgent agent;
        String creatureId = String.Empty;
        String creatureName = String.Empty;
		#endregion

		#region constructor
		public MainClass() {
			Application.Init();
			Console.WriteLine ("ClarionApp V0.8");
			try
            {
                ws = new WSProxy("localhost", 4011);

                String message = ws.Connect();

                if (ws != null && ws.IsConnected)
                {
                    Console.Out.WriteLine ("[SUCCESS] " + message + "\n");
					ws.SendWorldReset();
                    ws.NewCreature(400, 200, 0, out creatureId, out creatureName);
					ws.SendCreateLeaflet();

					int ws_width = 800;
					int ws_lenght = 600;

					ws.NewBrick(4, 0, 0, -50, ws_lenght+50);
					ws.NewBrick(4, 0, ws_lenght, ws_width+50, ws_lenght+50);
					ws.NewBrick(4, ws_width, ws_lenght, ws_width + 50, -50);
					ws.NewBrick(4, ws_width, 0, -50, -50);

					RandomGenerator rg = new RandomGenerator();

					int nu_of_jewels = 20;
					for(int i = 0; i < nu_of_jewels; i++){
						ws.NewJewel(rg.randomInt(0,5), rg.randomInt(0, ws_width), rg.randomInt(0, ws_lenght));
					}

					int nu_of_foods = 6;
					for(int i = 0; i < nu_of_foods; i++){
						ws.NewFood(rg.randomInt(0,1), rg.randomInt(0, ws_width), rg.randomInt(0, ws_lenght));
					}

                    if (!String.IsNullOrWhiteSpace(creatureId))
                    {
                        ws.SendStartCamera(creatureId);
                        ws.SendStartCreature(creatureId);
                    }

                    Console.Out.WriteLine("Creature created with name: " + creatureId + "\n");
					agent = new ClarionAgent(ws,creatureId,creatureName);
                    agent.Run();
					Console.Out.WriteLine("Running Simulation ...\n");
                }
				else {
					Console.Out.WriteLine("The WorldServer3D engine was not found ! You must start WorldServer3D before running this application !");
					System.Environment.Exit(1);
				}
            }
            catch (WorldServerInvalidArgument invalidArtgument)
            {
                Console.Out.WriteLine(String.Format("[ERROR] Invalid Argument: {0}\n", invalidArtgument.Message));
            }
            catch (WorldServerConnectionError serverError)
            {
                Console.Out.WriteLine(String.Format("[ERROR] Is is not possible to connect to server: {0}\n", serverError.Message));
            }
            catch (Exception ex)
            {
                Console.Out.WriteLine(String.Format("[ERROR] Unknown Error: {0}\n", ex.Message));
            }
			Application.Run();
		}
		#endregion

		#region Methods
		public static void Main (string[] args)	{
			new MainClass();
		}
			
        #endregion
	}
	
	
}
