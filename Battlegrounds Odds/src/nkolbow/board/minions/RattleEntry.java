package nkolbow.board.minions;

public class RattleEntry {
	
	private Deathrattle rattle;
	private int pos;
	private boolean isFriend;
	
	public RattleEntry(int pos, Deathrattle rattle, boolean isFriend) {
		this.rattle = rattle;
		this.pos = pos;
		this.isFriend = isFriend;
	}
	
	public Deathrattle getRattle() { return rattle; }
	public int getPos() { return this.pos; }
	public boolean getFriend() { return this.isFriend; }
	
	public RattleEntry clone() {
		return new RattleEntry(pos, rattle, isFriend);
	}
	
	public String toString() {
		return rattle.toString();
	}
	
	public int getTier() {
		if(rattle == Deathrattle.Process || rattle == Deathrattle.None)
			return 4;
		else if(rattle == Deathrattle.Gold_Spawn_of_NZoth || rattle == Deathrattle.Spawn_of_NZoth || rattle == Deathrattle.Goldrinn_the_Great_Wolf || rattle == Deathrattle.Gold_Goldrinn_the_Great_Wolf
				|| rattle == Deathrattle.Gold_Selfless_Hero || rattle == Deathrattle.Selfless_Hero || rattle == Deathrattle.Tortollan_Shellraiser || rattle == Deathrattle.Gold_Tortollan_Shellraiser)
			return 1;
		else if(rattle == Deathrattle.Gold_Kaboom_Bot || rattle == Deathrattle.Kaboom_Bot)
			return 2;
		return 3;
	}
	
}
