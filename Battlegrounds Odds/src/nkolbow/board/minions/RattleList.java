package nkolbow.board.minions;

import java.util.LinkedList;
import java.util.List;

public class RattleList {

	private LinkedList<LinkedList<Deathrattle>> depth;
	
	private LinkedList<Deathrattle> rattles;
	
	public RattleList() {
		rattles = new LinkedList<Deathrattle>();
		depth = new LinkedList<LinkedList<Deathrattle>>();
		
		depth.add(rattles);
	}
	
	public void addDepth() {
		depth.add(new LinkedList<Deathrattle>());
	}
	
	public void addAll(List<Deathrattle> rattles) {
		for(Deathrattle toAdd : rattles) {
			add(toAdd);
		}
	}
	
	public void add(Deathrattle rattle) {
		LinkedList<Deathrattle> addTo = depth.getLast();
		int tier = getTier(rattle);
		
		int i = 0;
		for(; i < addTo.size(); i++) {
			if(getTier(addTo.get(i)) > tier)
				break;
		}
		
		addTo.add(i, rattle);
	}
	
	/*
	 * ORDER OF DEATHRATTLES:
	 * 		1.  Spawn of N'Zoth
	 * 			Goldrinn, the Great Wolf
	 * 			Selfless Hero
	 * 			Tortollan Shellraiser
	 * 		2.	Kaboom Bot
	 * 		3.	Everything else (order is arbitrary, only summoning happens)
	 */
	
	public int getTier(Deathrattle rattle) {
		if(rattle == Deathrattle.Gold_Spawn_of_NZoth || rattle == Deathrattle.Spawn_of_NZoth || rattle == Deathrattle.Goldrinn_the_Great_Wolf || rattle == Deathrattle.Gold_Goldrinn_the_Great_Wolf
				|| rattle == Deathrattle.Gold_Selfless_Hero || rattle == Deathrattle.Selfless_Hero || rattle == Deathrattle.Tortollan_Shellraiser || rattle == Deathrattle.Gold_Tortollan_Shellraiser)
			return 1;
		else if(rattle == Deathrattle.Gold_Kaboom_Bot || rattle == Deathrattle.Kaboom_Bot)
			return 2;
		return 3;
	}
	
	public Deathrattle pop() {
		Deathrattle toRet = rattles.getFirst();
		rattles.remove(toRet);
		if(rattles.isEmpty())
			rattles = depth.get(1);
		
		depth.removeFirst();
		return toRet;
	}
	
	@Override
	public String toString() {
		String toRet = "";
		
		for(LinkedList<Deathrattle> list : depth) {
			for(Deathrattle rattle : list) {
				toRet += rattle.toString() + ", ";
			}
			toRet = toRet.substring(0, toRet.length() - 2);
			toRet += "\n";
		}
		
		return toRet;
	}
	
}
