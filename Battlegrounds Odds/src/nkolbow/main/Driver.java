package nkolbow.main;

import nkolbow.board.Board;
import nkolbow.board.FullBoardException;

public class Driver {

	public static void main(String[] args) {
		Board b = new Board(2, 2);
		
		try {
/*			// Kripparian double unlucky first battle
			b.addMinion("amalgom.a.3.4.2;;;", true, Integer.MAX_VALUE);
			b.addMinion("amalgom2.a.3.4.2;;;", true, Integer.MAX_VALUE);
			b.addMinion("tabby.b.3.3.1;;;", true, Integer.MAX_VALUE);
			b.addMinion("tabby2.b.1.1.1;;;", true, Integer.MAX_VALUE);
			b.addMinion("tabby_tok.b.1.1.1;;;", true, Integer.MAX_VALUE);
			
			b.addMinion("tabby.b.4.1.1;;;", false, Integer.MAX_VALUE);
			b.addMinion("tabby_tok.b.4.1.1;;;", false, Integer.MAX_VALUE);
			b.addMinion("rover.M.4.6.4;;;l", false, Integer.MAX_VALUE);
			b.addMinion("leaper.M.3.3.2;;;", false, Integer.MAX_VALUE);
			b.addMinion("pack_leader..3.3.3;;;g", false, Integer.MAX_VALUE);
			b.addMinion("tide.m.2.1.1;;;", false, Integer.MAX_VALUE);			*/
			
			// second battle
//			b.addMinion(minion, isFriend, position);
			
			
			// Deathrattle testing (yay!)
			b.addMinion("rat_pack.b.1.1.1;;G;", true, Integer.MAX_VALUE);
			
			b.addMinion("irrel..1.1.1;;;", false, Integer.MAX_VALUE);
			
			
/*			// Testing security rover
			b.addMinion("rover.M.1.2.4;;;L", true, Integer.MAX_VALUE);
			
			b.addMinion("irrel..1.1.1;;;", false, Integer.MAX_VALUE);
			b.addMinion("irrel..6.1.1;;;", false, Integer.MAX_VALUE);
			// <Testing security rover>											*/
			
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
		} catch (FullBoardException e) {
			e.printStackTrace();
		}
		
		Board.battle(b);
	}
	
}
