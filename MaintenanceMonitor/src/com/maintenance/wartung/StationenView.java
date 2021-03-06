package com.maintenance.wartung;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import com.maintenance.util.ProzentCalc;
import com.maintenancemonitor.db.dao.AnlageJDBCDAO;
import com.maintenancemonitor.db.dao.StationJDBCDAO;
import com.maintenancemonitor.db.dao.WartungDAO;
import com.maintenancemonitor.db.dao.WartungJDBCDAO;
import com.maintenancemonitor.db.dto.AnlageDTO;
import com.maintenancemonitor.db.dto.StationDTO;
import com.maintenancemonitor.db.dto.WartungDTO;
import com.maintenancemonitor.db.dto.WartungDTO.EWartungArt;
import com.maintenancemonitor.util.DAOException;

@ManagedBean(name = "stationenView")
@RequestScoped
public class StationenView {

	private int count;

	private List<StationDTO> stationen;
	private WartungDTO wartung;

	@ManagedProperty(value = "#{station}")
	private StationDTO station;

	public StationenView() {

		StationJDBCDAO stationDAO = new StationJDBCDAO();
		AnlageJDBCDAO anlageDAO = new AnlageJDBCDAO();

		try {

			List<StationDTO> stationen = stationDAO.getStationen();

			for (AnlageDTO anl : anlageDAO.getAnlagen()) {
				for (StationDTO st : stationen) {
					if (anl.getId() == st.getAnlageId()) {
						st.setAnlage(anl);
					}

				}

			}

			this.stationen = new ArrayList<>();

			for (StationDTO st : stationen) {

				if (st.isTpm())
					if (checkStationElapsed(st))
						this.stationen.add(st);

			}

		} catch (DAOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String select() {

		System.out.println("select: " + station.getName());

		return "tpmWartung";

	}

	public List<StationDTO> getStationen() {

		return stationen;

	}

	public StationDTO getStation() {

		return station;
	}

	public void setStation(StationDTO station) {

		this.station = station;
	}

	public WartungDTO getWartung() {
		return wartung;
	}

	public void setWartung(WartungDTO wartung) {
		this.wartung = wartung;
	}

	public void countUp() {

		count++;

		System.out.println(count);
	}

	public String save() {

		try {
			WartungDAO wartungDAO = new WartungJDBCDAO();
			wartung.setStationId(station.getId());

			System.out.println("Wartung speichern");
			System.out.println("Datum: " + wartung.getFaellig());
			System.out.println("Mitarbeiter: " + wartung.getMitarbeiter());
			System.out.println("Info: " + wartung.getInfo());
			System.out.println("StationId: " + wartung.getStationId());

			wartungDAO.insertWartung(wartung);
		} catch (DAOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "tpmWartungen";
	}

	private boolean checkStationElapsed(StationDTO station) {

		String remark = null;
		float prozent = 0;
		boolean maintenanceElapsed = false;

		if (station.getWartungArt() == EWartungArt.STUECKZAHL.ordinal()) {

			prozent = ProzentCalc.calcProzent(station);

			if (prozent >= station.getWartungStueckWarnung() && prozent < station.getWartungStueckFehler())
				maintenanceElapsed = true;

			else if (prozent >= station.getWartungStueckFehler())
				maintenanceElapsed = true;
		}

		if (station.getWartungArt() == EWartungArt.TIME_INTERVALL.ordinal()) {

			if (station.getCreateDate() != null || station.getLastWartungDate() != null) {

				Date nextWarnungDate = null;
				Date nextWartungDate;

				if (station.getLastWartungDate() != null) {
					nextWartungDate = ProzentCalc.calcNextWartungDate(station.getLastWartungDate(),
							station.getIntervallDateUnit(), station.getWartungDateIntervall());
					nextWarnungDate = ProzentCalc.calcNextWarnungDate(station.getWarnungDateUnit(),
							station.getLastWartungDate(), nextWartungDate, station.getWartungDateWarnung());
					prozent = ProzentCalc.calcProzent(station.getLastWartungDate().getTime(),
							nextWartungDate.getTime());
				} else {
					nextWartungDate = ProzentCalc.calcNextWartungDate(station.getCreateDate(),
							station.getIntervallDateUnit(), station.getWartungDateIntervall());
					nextWarnungDate = ProzentCalc.calcNextWarnungDate(station.getWarnungDateUnit(),
							station.getCreateDate(), nextWartungDate, station.getWartungDateWarnung());
					prozent = ProzentCalc.calcProzent(station.getCreateDate().getTime(), nextWartungDate.getTime());
				}

				SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
				remark = df.format(nextWartungDate);

				if (Calendar.getInstance().getTime().after(nextWarnungDate)
						&& Calendar.getInstance().getTime().before(nextWartungDate))
					maintenanceElapsed = true;

				if (Calendar.getInstance().getTime().after(nextWartungDate))
					maintenanceElapsed = true;

			}

		}

		return maintenanceElapsed;

	}

}