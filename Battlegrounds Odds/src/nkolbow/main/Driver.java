package nkolbow.main;

import nkolbow.board.Board;
import nkolbow.board.minions.Min;
import nkolbow.board.minions.deathrattles.Deathrattle;

public class Driver {

	public static void main(String[] args) {
		Board b = new Board(4, 4);
		
		try {
			// TODO: FIGURE OUT WHY THE FUCK DEMON DEATHRATTLE IS TRIGGERING TWICE ON THAT SILLY LITTLE IMP
			
			b.addMinion(true, Min.Selfless_Hero, false, 1, 1, false, false, false, Deathrattle.Gold_Selfless_Hero);
			b.addMinion(true, Min.Soul_Juggler, true, 3, 3, true, false, false);
			b.addMinion(true, Min.Soul_Juggler, true, 3, 3, true, false, false);
			b.addMinion(true, Min.Soul_Juggler, false, 3, 3, true, false, false);
			b.addMinion(true, Min.Imp, false, 1, 1, true, false, false);
			b.addMinion(true, Min.Imp, false, 1, 1, false, false, false);

			b.addMinion(false, Min.Alleycat, false, 7, 1, false, false, false);
			
			// stfu kripp
//			b.addMinion("goldrinn.b.5.5.5;;o;", true, Integer.MAX_VALUE);
//			b.addMinion("cobalt.M.10.5.3;d;;d", true, Integer.MAX_VALUE);
//			b.addMinion("amalgom.a.11.5.2;;k;", true, Integer.MAX_VALUE);
//			b.addMinion("amalgom.a.7.4.2;p;;", true, Integer.MAX_VALUE);
//			b.addMinion("sensei.M.4.2.4;;;", true, Integer.MAX_VALUE);
//			b.addMinion("leaper.M.3.3.2;;;", true, Integer.MAX_VALUE);
//			
//			b.addMinion("eye.m.15.9.2;;;c", false, Integer.MAX_VALUE);
//			b.addMinion("eye.m.14.6.2;;;c", false, Integer.MAX_VALUE);
//			b.addMinion("amalgom.a.7.8.2;;;", false, Integer.MAX_VALUE);
//			b.addMinion("coldlight.m.6.5.2;;;", false, Integer.MAX_VALUE);
//			b.addMinion("coldlight.m.6.3.2;;;", false, Integer.MAX_VALUE);
//			b.addMinion("war.m.5.5.3;;;b", false, Integer.MAX_VALUE);
//			b.addMinion("war.m.5.5.3;;;b", false, Integer.MAX_VALUE);
			
			
			
			// krip n_l
//			b.addMinion("bag.m.6.15.5;p;w;", true, Integer.MAX_VALUE);
//			b.addMinion("bag.m.8.21.5;p;w;", true, Integer.MAX_VALUE);
//			b.addMinion("mur.m.8.23.4;p;;", true, Integer.MAX_VALUE);
//			b.addMinion("mur.m.11.27.1;p;;", true, Integer.MAX_VALUE);
//			b.addMinion("mur.m.12.28.1;p;;", true, Integer.MAX_VALUE);
//			b.addMinion("brann..2.4.5;;;", true, Integer.MAX_VALUE);
//			b.addMinion("cold_light.m.2.3.2;;;", true, Integer.MAX_VALUE);
//			
//			b.addMinion("nzoth..4.4.2;;h;", false, Integer.MAX_VALUE);
//			b.addMinion("mur.m.4.8.1;p;;", false, Integer.MAX_VALUE);
//			b.addMinion("brann..2.4.5;;;", false, Integer.MAX_VALUE);
//			b.addMinion("mech.M.2.5.3;;;", false, Integer.MAX_VALUE);
//			b.addMinion("b..4.5.3;;;", false, Integer.MAX_VALUE);
//			b.addMinion("rover.M.11.13.4;;k;l", false, Integer.MAX_VALUE);
//			b.addMinion("amalgom.a.18.21.2;ptd;;", false, Integer.MAX_VALUE);
			
			
			
			// The beast
//			b.addMinion("the_beast.b.1.1.1;;n;", true, Integer.MAX_VALUE);
//			b.addMinion("khadgar..1.1.1;;;f", true, Integer.MAX_VALUE);
//			b.addMinion("rivendare..1.1.1;;;n", true, Integer.MAX_VALUE);
//			
//			b.addMinion("oofta..1.5.1;;;", false, Integer.MAX_VALUE);
			
			
			
			// Replicating menace & cobalt
//			b.addMinion("menace.M.3.1.3;;k;", true, Integer.MAX_VALUE);
//			b.addMinion("cobalt.M.6.3.3;t;;d", true, Integer.MAX_VALUE);
//			
//			b.addMinion("irrel..3.9.1;;;", false, Integer.MAX_VALUE);
			
			
			
			// KRIPPARIAN'S SECOND "UNLUCKY" SITUATION
//			b.addMinion("amalgom.a.6.9.2;;;", true, Integer.MAX_VALUE);
//			b.addMinion("amalgom.a.5.8.2;;;", true, Integer.MAX_VALUE);
//			b.addMinion("tabby.b.4.4.1;;;", true, Integer.MAX_VALUE);
//			b.addMinion("coldlight.m.2.5.2;;;", true, Integer.MAX_VALUE);
//			b.addMinion("coldlight.m.3.4.2;;;", true, Integer.MAX_VALUE);
//			b.addMinion("leaper.M.3.3.2;;;", true, Integer.MAX_VALUE);
//			
//			b.addMinion("tabby.b.4.1.1;;;", false, Integer.MAX_VALUE);
//			b.addMinion("tabby.b.4.1.1;;;", false, Integer.MAX_VALUE);
//			b.addMinion("rover.M.4.6.4;;;l", false, Integer.MAX_VALUE);
//			b.addMinion("leaper.M.3.3.2;;;", false, Integer.MAX_VALUE);
//			b.addMinion("rat_pack.b.5.2.2;;g;", false, Integer.MAX_VALUE);
//			b.addMinion("pack_leader..3.3.3;;;g", false, Integer.MAX_VALUE);
//			b.addMinion("pack_leader..3.3.3;;;g", false, Integer.MAX_VALUE);
			
			
			// Rat pack
//			b.addMinion("irrel..1.10.1;;;", true, Integer.MAX_VALUE);
//			b.addMinion("ratpack.b.2.1.1;t;g;", true, Integer.MAX_VALUE);
//			b.addMinion("pack_leader..3.3.3;;;g", true, Integer.MAX_VALUE);
//			b.addMinion("pack_leader..3.3.3;;;g", true, Integer.MAX_VALUE);
//			
//			b.addMinion("irrel..1.2.1;;;", false, Integer.MAX_VALUE);
			
			
			
			
			// Baron riven
//			b.addMinion("tortollan..1.1.3;t;L;", true, Integer.MAX_VALUE);
//			b.addMinion("baron..1.1.5;;;N", true, Integer.MAX_VALUE);
//			
//			b.addMinion("irrel..2.4.1;;;", false, Integer.MAX_VALUE);
			
			
			
			
			// Golden Tortollan
//			b.addMinion("tortollan..1.1.3;;L;", true, Integer.MAX_VALUE);
//			b.addMinion("irrel..4.1.1;;;", true, Integer.MAX_VALUE);
//			b.addMinion("irrel..4.1.1;;;", true, Integer.MAX_VALUE);
//			b.addMinion("irrel..4.1.1;;;", true, Integer.MAX_VALUE);
//			b.addMinion("irrel..4.1.1;;;", true, Integer.MAX_VALUE);
//			
//			b.addMinion("irrel..10.10.10;;;", false, Integer.MAX_VALUE);
			
			
			// Tortollan
//			b.addMinion("tortollan..1.1.3;;l;", true, Integer.MAX_VALUE);
//			b.addMinion("irrel..3.1.1;d;;", true, Integer.MAX_VALUE);
//			b.addMinion("irrel..1.1.1;;;", true, Integer.MAX_VALUE);
//			
//			b.addMinion("irrel..10.10.10;;;", false, Integer.MAX_VALUE);
			
			
			
			// Goldrinn my dude
//			b.addMinion("gold.b.4.4.5;t;o;", true, Integer.MAX_VALUE);
//			b.addMinion("hydra.b.2.4.4;c;;", true, Integer.MAX_VALUE);
//			b.addMinion("hydra.b.2.4.4;c;;", true, Integer.MAX_VALUE);
//			b.addMinion("hydra.b.2.4.4;c;;", true, Integer.MAX_VALUE);
//			
//			b.addMinion("tabby.b.4.50.1;;;", false, Integer.MAX_VALUE);
			
			
			
			// Super preliminary deathrattle testing
//			b.addMinion("spawn..2.2.1;t;h;", true, Integer.MAX_VALUE);
//			b.addMinion("spawn..2.2.1;t;h;", true, Integer.MAX_VALUE);
//			b.addMinion("spawn..2.2.1;t;h;", true, Integer.MAX_VALUE);
//			b.addMinion("spawn..2.2.1;t;h;", true, Integer.MAX_VALUE);
//
//			b.addMinion("spawn..2.2.1;t;h;", false, Integer.MAX_VALUE);
//			b.addMinion("spawn..2.2.1;t;h;", false, Integer.MAX_VALUE);
//			b.addMinion("spawn..2.2.1;t;h;", false, Integer.MAX_VALUE);
//			b.addMinion("spawn..2.2.1;t;h;", false, Integer.MAX_VALUE);

			
			
			
			// Testerinos
//			b.addMinion("tabby.b.1.5.1;;;", true, Integer.MAX_VALUE);
//
//			b.addMinion("tabby.b.1.1.1;t;h;", false, Integer.MAX_VALUE);
//			b.addMinion("notTabby.a.1.1.1;p;;", false, Integer.MAX_VALUE);
			
			
			
			
/*			// Testing RattleList
			RattleList list = new RattleList();
			list.add(Deathrattle.Gold_Kaboom_Bot);
			list.add(Deathrattle.Gold_Piloted_Shredder);
			list.add(Deathrattle.Gold_Kaboom_Bot);
			list.add(Deathrattle.Ghastcoiler);
			list.add(Deathrattle.Gold_Goldrinn_the_Great_Wolf);
			list.add(Deathrattle.Ghastcoiler);
			
			list.addDepth();
			
			list.add(Deathrattle.Gold_Kaboom_Bot);
			list.add(Deathrattle.Gold_Selfless_Hero);
			list.add(Deathrattle.Goldrinn_the_Great_Wolf);
			list.add(Deathrattle.Gold_The_Beast);
			
			list.pop();
			System.out.println(list.toString());					*/
			
			
/*			b.addMinion("foe.M.21.22.6;c;;", true, Integer.MAX_VALUE);
			b.addMinion("cobalt.M.30.23.3;d;;", true, Integer.MAX_VALUE);
			b.addMinion("murloc.m.17.20.1;pd;;", true, Integer.MAX_VALUE);
			b.addMinion("iron_sensei.M.21.15.4;;;", true, Integer.MAX_VALUE);
			b.addMinion("micro1.M.1.1.1;;;", true, Integer.MAX_VALUE);
			
			b.addMinion("summ.M.8.7.1;;;", false, Integer.MAX_VALUE);
			b.addMinion("amalgom.a.54.45.2;pt;;", false, Integer.MAX_VALUE);
			b.addMinion("battle.d.9.40.5;;;", false, Integer.MAX_VALUE);
			
			b.getFriends()._attacking = 1;
			b.getEnemies()._attacking = 1;										*/
			
			
			// Kripparian double unlucky first battle
//			b.addMinion("amalgom.a.3.4.2;;;", true, Integer.MAX_VALUE);
//			b.addMinion("amalgom2.a.3.4.2;;;", true, Integer.MAX_VALUE);
//			b.addMinion("tabby.b.3.3.1;;;", true, Integer.MAX_VALUE);
//			b.addMinion("tabby2.b.1.1.1;;;", true, Integer.MAX_VALUE);
//			b.addMinion("tabby_tok.b.1.1.1;;;", true, Integer.MAX_VALUE);
//			
//			b.addMinion("tabby.b.4.1.1;;;", false, Integer.MAX_VALUE);
//			b.addMinion("tabby_tok.b.4.1.1;;;", false, Integer.MAX_VALUE);
//			b.addMinion("rover.M.4.6.4;;;l", false, Integer.MAX_VALUE);
//			b.addMinion("leaper.M.3.3.2;;;", false, Integer.MAX_VALUE);
//			b.addMinion("pack_leader..3.3.3;;;g", false, Integer.MAX_VALUE);
//			b.addMinion("tide.m.2.1.1;;;", false, Integer.MAX_VALUE);			
			
			// second battle
//			b.addMinion(minion, isFriend, position);
			
			

/*			// Deathrattle testing (yay!)
			// Should be 100% win
			b.addMinion("rat_pack.b.1.1.1;;G;", true, Integer.MAX_VALUE);
			
			b.addMinion("irrel..1.1.1;;;", false, Integer.MAX_VALUE);			*/
			
			
			
			// Testing security rover
//			b.addMinion("rover.M.1.2.4;;;L", true, Integer.MAX_VALUE);
//			
//			b.addMinion("irrel..1.1.1;;;", false, Integer.MAX_VALUE);
//			b.addMinion("irrel..6.1.1;;;", false, Integer.MAX_VALUE);
			// <Testing security rover>											
			
			// Kripparian's situation; IRON SENSEI IS NO JOKE <12:00>
/*			b.addMinion("cobat.M.12.7.3;;;", true, Integer.MAX_VALUE);
			b.addMinion("mur.m.10.11.1;p;;", true, Integer.MAX_VALUE);
			b.addMinion("sens.M.6.4.4;;;", true, Integer.MAX_VALUE);
			b.addMinion("sens2.M.10.8.3;;;", true, Integer.MAX_VALUE);
			b.addMinion("argus..2.3.4;;;", true, Integer.MAX_VALUE);
			
			b.addMinion("mur.m.8.13.1;;;", false, Integer.MAX_VALUE);
			b.addMinion("war.m.9.10.2;;;b", false, Integer.MAX_VALUE);
			b.addMinion("amalgom.a.14.4.2;pt;;", false, Integer.MAX_VALUE);
			
			b.getFriends()._attacking = 1;                                      */
			// </Kripparian's situation; IRON SENSEI IS NO JOKE <12:00>>
			
			
			/*b.addMinion("imp.d.1.1.1;d;;", true, Integer.MAX_VALUE);
			//b.addMinion("malganis.d.1.1.5;;;q", true, Integer.MAX_VALUE);
			//b.addMinion("malganis.d.0.1.5;;;Q", true, Integer.MAX_VALUE);
			
			b.addMinion("mama.b.1.10.1;pd;;G", false, Integer.MAX_VALUE);
			b.addMinion("mama.b.1.10.1;pd;;S", false, Integer.MAX_VALUE);*/
			
			//b.addMinion("cobalt.M.18.6.3;;;", true, Integer.MAX_VALUE);
			//b.addMinion("taunt1.d.6.8.1;pdc;;", true, Integer.MAX_VALUE);
			//b.addMinion("m4.M.7.7.2;;;", true, Integer.MAX_VALUE);
			//b.addMinion("micro.M.40.5.1;d;;", true, Integer.MAX_VALUE);
			//b.addMinion("taunt1.d.6.8.1;pd;;", true, Integer.MAX_VALUE);
//			b.addMinion("m4.M.7.7.2;;;", true, Integer.MAX_VALUE);
//			b.addMinion("micro.M.40.5.1;;;", true, Integer.MAX_VALUE);
			
			//b.addMinion("battle.demon.6.37.1;t;;", false, Integer.MAX_VALUE);
//			b.addMinion("void.demon.3.9.1;t;;", false, Integer.MAX_VALUE);
//			b.addMinion("zap..7.10.1;t;;", false, Integer.MAX_VALUE);
//			b.addMinion("dem.demon.14.14.1;t;;", false, Integer.MAX_VALUE);
//			b.addMinion("battle.demon.6.37.1;;;", false, Integer.MAX_VALUE);
//			b.addMinion("void.demon.3.9.1;;;", false, Integer.MAX_VALUE);
//			b.addMinion("zap..7.10.1;;;", false, Integer.MAX_VALUE);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Board.battle(b);
	}
	
}
