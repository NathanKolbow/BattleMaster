package nkolbow.board.minions;

import java.util.ArrayList;

import nkolbow.board.Board;

public class Minion {
	
	public enum Tribe {
		Beast, Murloc, Mech, Dragon, Demon, All, None
	}
	
	private String name;
	
	private Board board;
	
	private int health;
	private int maxHealth;
	private int attack;
	private int ID;
	private int stars;
	
	private Line line;
	
	private Tribe tribe;
	private Effect effect;
	
	private boolean hasDivine;
	private boolean hasPoison;
	private boolean hasCleave;
	private boolean hasTaunt;
	
	private ArrayList<Deathrattle> deathrattles;
	
	public Minion(String name, Tribe tribe, Line line, int attack, int health, int maxHealth, int stars, Board board,
					Effect effect, boolean hasDivine, boolean hasPoison, boolean hasCleave, boolean hasTaunt, ArrayList<Deathrattle> deathrattles) {
		this.name = name;
		this.tribe = tribe;
		this.line = line;
		this.attack = attack;
		this.health = health;
		this.maxHealth = maxHealth;
		this.stars = stars;
		this.board = board;
		this.effect = effect;
		this.hasDivine = hasDivine;
		this.hasPoison = hasPoison;
		this.hasCleave = hasCleave;
		this.hasTaunt = hasTaunt;
		
		this.deathrattles = (deathrattles == null) ? new ArrayList<Deathrattle>() : deathrattles;
	}
	
	public void setDeathrattles(ArrayList<Deathrattle> deaths) { this.deathrattles = deaths; }
	public ArrayList<Deathrattle> getDeathrattles() { return this.deathrattles; }
	
	public void setEffect(Effect e) { this.effect = e; }
	public Effect getEffect() { return this.effect; }
	public Tribe getTribe() { return this.tribe; }
	public void setDivine(boolean b) { this.hasDivine = b; }
	public boolean hasDivine() { return this.hasDivine; }
	public void setPoison(boolean b) { this.hasPoison = b; }
	public boolean hasPoison() { return this.hasPoison; }
	public void setCleave(boolean b) { this.hasCleave = b; }
	public boolean hasCleave() { return this.hasCleave; }
	public void setTaunt(boolean b) { this.hasTaunt = b; }
	public boolean hasTaunt() { return this.hasTaunt; }
	
	public boolean isDead(Line l) { return this.getHealth(l) <= 0; }
	
	public String getName() { return this.name; }
	
	public Board getBoard() { return this.board; }
	public Line getLine() { return this.line; }
	
	public int getStars() { return this.stars; }
	public void setBaseHealth(int h) { this.health = h; }
	public int getBaseHealth() { return this.health; }
	public int getMaxHealth() { return this.maxHealth; }
	public void setBaseAttack(int a) { this.attack = a; }
	public int getBaseAttack() { return this.attack; }
	
	public int getHealth(Line l) {
		int finalHealth = health;
		if(this.tribe == Tribe.Demon) {
			for(Minion m : l.getMinions()) {
				if(m == this) continue;
				
				if(m.effect == Effect.MalGanis)
					finalHealth += 2;
				if(m.effect == Effect.Gold_MalGanis)
					finalHealth += 4;
			}
		}
		
		return finalHealth;
	}
	
	public int getAttack(Line l) {
		int finalAtt = attack;
		
		ArrayList<Minion> adj = l.getAdjMinions(this);
		for(Minion m : adj) {
			if(m.effect == Effect.Dire_Wolf_Alpha)
				finalAtt += 2;
			if(m.effect == Effect.Gold_Dire_Wolf_Alpha)
				finalAtt += 4;
		}
		
		for(Minion m : l.getMinions()) {
			if(m == this) continue;
			
			if(this.tribe == Tribe.Murloc) {
				if(m.effect == Effect.Murloc_Warleader)
					finalAtt += 2;
				if(m.effect == Effect.Gold_Murloc_Warleader)
					finalAtt += 4;
			}
		}
		
		if(this.effect == Effect.Old_Murk_Eye) {
			for(Minion min : l.getMinions())
				if(min != this)
					if(min.tribe == Tribe.Murloc)
						finalAtt++;
		}
		if(this.effect == Effect.Gold_Old_Murk_Eye) {
			for(Minion min : l.getMinions())
				if(min != this)
					if(min.tribe == Tribe.Murloc)
						finalAtt += 2;
		}
		
		if(this.hasTaunt) {
			for(Minion min : l.getMinions()) {
				if(min == this) continue;
				
				if(min.effect == Effect.Phalanx_Commander)
					finalAtt += 2;
				if(min.effect == Effect.Gold_Phalanx_Commander)
					finalAtt += 4;
			}
		}
		
		if(this.tribe == Tribe.Demon) {
			for(Minion m : l.getMinions()) {
				if(m == this) continue;
				
				if(m.effect == Effect.Siegebreaker)
					finalAtt++;
				if(m.effect == Effect.Gold_Siegebreaker)
					finalAtt += 2;
				
				if(m.effect == Effect.MalGanis)
					finalAtt += 2;
				if(m.effect == Effect.Gold_MalGanis)
					finalAtt += 4;
			}
		}
		
		return finalAtt;
	}
	
	public void takePoison(int d) {
		if(d == 0)
			return;
		
		if(this.hasDivine) {
			this.setDivine(false);
		} else {
			this.takeDamage(15000);
		}
	}
	
	public void takeDamage(int d) { 
		if(d == 0)
			return;
		
		if(this.hasDivine) {
			this.setDivine(false);
		} else {
			this.health -= d;
			
			if(this.effect == Effect.Security_Rover) {
				int _ind = this.line.getMinions().indexOf(this) + 1;
				
				Minion rover = new Minion("rover.token", Tribe.Mech, this.line, 2, 3, 3, 1, this.board, Effect.None, false, false, false, true, null);
				this.line.summon(rover, _ind, this.line);
			} else if(this.effect == Effect.Gold_Security_Rover) {
				int _ind = this.line.getMinions().indexOf(this) + 1;
				
				Minion rover = new Minion("rover.gold.token", Tribe.Mech, this.line, 4, 6, 6, 1, this.board, Effect.None, false, false, false, true, null);
				this.line.summon(rover,  _ind, this.line);
			}
		}
	}
	
	/**
	 * @return - whether or not this minion summons other minions when it dies
	 */
	public boolean summons() {
		if(deathrattles.isEmpty()) return false;
		return deathrattles.contains(Deathrattle.Mecharoo) || deathrattles.contains(Deathrattle.Gold_Mecharoo) || deathrattles.contains(Deathrattle.Harvest_Golem) || deathrattles.contains(Deathrattle.Gold_Harvest_Golem) || deathrattles.contains(Deathrattle.Gold_Kindly_Grandmother) || deathrattles.contains(Deathrattle.Kindly_Grandmother)
				|| deathrattles.contains(Deathrattle.Gold_Mounted_Raptor) || deathrattles.contains(Deathrattle.Mounted_Raptor) || deathrattles.contains(Deathrattle.Rat_Pack) || deathrattles.contains(Deathrattle.Gold_Rat_Pack) || deathrattles.contains(Deathrattle.Infested_Wolf) || deathrattles.contains(Deathrattle.Gold_Infested_Wolf)
				|| deathrattles.contains(Deathrattle.Gold_Piloted_Shredder) || deathrattles.contains(Deathrattle.Gold_Piloted_Shredder) || deathrattles.contains(Deathrattle.Gold_Replicating_Menace) || deathrattles.contains(Deathrattle.Replicating_Menace) || deathrattles.contains(Deathrattle.Gold_Piloted_Sky_Golem) || deathrattles.contains(Deathrattle.Piloted_Sky_Golem)
				|| deathrattles.contains(Deathrattle.Gold_The_Beast) || deathrattles.contains(Deathrattle.The_Beast) || deathrattles.contains(Deathrattle.Mechano_egg) || deathrattles.contains(Deathrattle.Gold_Mechano_egg) || deathrattles.contains(Deathrattle.Sated_Threshadon) || deathrattles.contains(Deathrattle.Gold_Sated_Threshadon)
				|| deathrattles.contains(Deathrattle.Gold_Savannah_Highmane) || deathrattles.contains(Deathrattle.Savannah_Highmane) || deathrattles.contains(Deathrattle.Gold_Voidlord) || deathrattles.contains(Deathrattle.Voidlord) || deathrattles.contains(Deathrattle.Ghastcoiler) || deathrattles.contains(Deathrattle.Gold_Ghastcoiler)
				|| deathrattles.contains(Deathrattle.Kangors_Apprentice) || deathrattles.contains(Deathrattle.Gold_Kangors_Apprentice) || deathrattles.contains(Deathrattle.Gold_Sneeds_Old_Shredder) || deathrattles.contains(Deathrattle.Sneeds_Old_Shredder);
	}

	/**
	 * @return - the total amount of minions that this minion summons when it dies
	 */
	public int getTotalSummons() {
		return 0;
	}
	
	/**
	 * Creates a deep clone of the minion for the new board specified
	 * 
	 * @param b - new board
	 * @return - deep clone
	 */
	public Minion clone(Board b, Line l) {
		ArrayList<Deathrattle> newRattles = new ArrayList<Deathrattle>();
		for(Deathrattle rattle : deathrattles)
			newRattles.add(rattle);
		
		Minion toRet = new Minion(new String(this.name), this.tribe, l, this.attack, this.health, this.maxHealth, this.stars, b, this.effect,
									this.hasDivine, this.hasPoison, this.hasCleave, this.hasTaunt, newRattles);

		return toRet;
	}
	
	public boolean equals(Minion m) {
		return this.ID == m.ID;
	}
	
	public String toString() {
		return name + "." + this.getAttack(line) + "/" + getHealth(line) + ";" + ((hasTaunt) ? "t" : "") + ((hasDivine) ? "d" : "") + ((hasPoison) ? "p" : "") + ((hasCleave) ? "c" : "") + ";";
	}
	
}
