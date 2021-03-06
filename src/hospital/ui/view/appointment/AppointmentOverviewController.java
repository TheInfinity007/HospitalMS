package hospital.ui.view.appointment;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Predicate;

import com.jfoenix.controls.JFXComboBox;

import hospital.model.Appointment;
import hospital.model.Doctor;
import hospital.model.Patient;
import hospital.services.AppointmentSql;
import hospital.ui.main.Main;
import hospital.ui.view.doctor.DoctorDetailsController;
import hospital.ui.view.patient.PatientDetailsController;
import hospital.util.DateTimeUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AppointmentOverviewController {

	private final String rangeAll = "All";
	private final String rangeUpcoming = "Upcoming";
	private final String rangeToday = "Today";
	private final String rangePast = "Past";
	private ObservableList<Appointment> observableList = FXCollections.observableArrayList();
	private FilteredList<Appointment> filteredList = null;
	private SortedList<Appointment> sortedList = null;
	public AnchorPane overlay = null;

	public ObservableList<Appointment> getObservableList() {
		return this.observableList;
	}

	public void setObservableList(ObservableList<Appointment> observableList) {
		this.observableList = observableList;
	}

	public FilteredList<Appointment> getFilteredList() {
		return this.filteredList;
	}

	public void setFilteredList(FilteredList<Appointment> filteredList) {
		this.filteredList = filteredList;
	}

	public SortedList<Appointment> getSortedList() {
		return this.sortedList;
	}

	public void setSortedList(SortedList<Appointment> sortedList) {
		this.sortedList = sortedList;
	}

	@FXML
	private TableView<Appointment> tableView;
	@FXML
	private TableColumn<Appointment, String> idTableColumn;
	@FXML
	private TableColumn<Appointment, String> patientIDTableColumn;
	@FXML
	private TableColumn<Appointment, String> doctorIDTableColumn;
	@FXML
	private TableColumn<Appointment, LocalDateTime> dateTableColumn;
	@FXML
	private Label appointIDLbl;
	@FXML
	private Label patientIDLbl;
	@FXML
	private Label dateLbl;
	@FXML
	private Label doctorIDLbl;
	@FXML
	private Label viewSwitch;
	@FXML
	private TextField filterTF;
	@FXML
	private Button edit;
	@FXML
	private ButtonBar buttonBar;
	@FXML
	private JFXComboBox<String> filterCB;

	public AppointmentOverviewController() {
		observableList.addAll(AppointmentSql.getAppointments());
	}

	@FXML
	private void initialize() {
		patientIDTableColumn.setCellValueFactory(new PropertyValueFactory<Appointment, String>("patientID"));
		idTableColumn.setCellValueFactory(new PropertyValueFactory<Appointment, String>("id"));
		doctorIDTableColumn.setCellValueFactory(new PropertyValueFactory<Appointment, String>("doctorID"));
		dateTableColumn.setCellValueFactory(new PropertyValueFactory<Appointment, LocalDateTime>("date"));

		/* Event listeners for cells */
		patientIDTableColumn.setCellFactory(tc -> {
			return patientCellFactory();
		});
		doctorIDTableColumn.setCellFactory(tc -> {
			return doctorCellFactory();
		});
		idTableColumn.setCellFactory(tc -> {
			return idCellFactory();
		});
		dateTableColumn.setCellFactory(tc -> {
			return dateTimeCellFactory();
		});

		filterCB.getItems().add(rangeAll);
		filterCB.getItems().add(rangeUpcoming);
		filterCB.getItems().add(rangeToday);
		filterCB.getItems().add(rangePast);
		filterCB.setValue(rangeAll);

		ObjectProperty<Predicate<Appointment>> tfFilter = new SimpleObjectProperty<>();
		tfFilter.bind(Bindings.createObjectBinding(() -> appointment -> {
			if (filterTF.getText() == null || filterTF.getText().isEmpty())
				return true;
			String filter = filterTF.getText().toLowerCase();
			if (appointment.getId().toLowerCase().contains(filter))
				return true;
			if (appointment.getDoctorID().toLowerCase().contains(filter))
				return true;
			if (appointment.getPatientID().toLowerCase().contains(filter))
				return true;
			return false;
		}, filterTF.textProperty()));

		ObjectProperty<Predicate<Appointment>> cbFilter = new SimpleObjectProperty<>();
		cbFilter.bind(Bindings.createObjectBinding(() -> appointment -> {
			LocalDateTime date = appointment.getDate();
			if (filterCB.getValue().equals(rangeAll))
				return true;
			if (filterCB.getValue().equals(rangeUpcoming)) {
				if (date.toLocalDate().isAfter(LocalDate.now()) || date.toLocalDate().equals(LocalDate.now())) {
					return true;
				}
			}
			if (filterCB.getValue().equals(rangeToday)) {
				if (date.toLocalDate().equals(LocalDate.now())) {
					return true;
				}
			}
			if (filterCB.getValue().equals(rangePast)) {
				if (date.toLocalDate().isBefore(LocalDate.now())) {
					return true;
				}
			}

			return false;
		}, filterCB.valueProperty()));

		/* Filter table */
		filteredList = new FilteredList<Appointment>(observableList, p -> true);
		filteredList.predicateProperty()
				.bind(Bindings.createObjectBinding(() -> tfFilter.get().and(cbFilter.get()), tfFilter, cbFilter));

		sortedList = new SortedList<Appointment>(filteredList);
		sortedList.comparatorProperty().bind(tableView.comparatorProperty());
		tableView.setItems(sortedList);

		// show empty in personal details
		showAppointmentDetails(null);
		tableView.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> showAppointmentDetails(newValue));

		// Clear Selection On Opening
		tableView.getSelectionModel().clearSelection();

		// Clear Selection when clicking on empty rows
		ObjectProperty<TableRow<Appointment>> lastSelectedRow = new SimpleObjectProperty<>();
		tableView.setRowFactory(tableView -> {
			TableRow<Appointment> row = new TableRow<Appointment>();
			row.selectedProperty().addListener((observable, wasSelected, isNowSelected) -> {
				if (isNowSelected) {
					lastSelectedRow.set(row);
				}
			});
			return row;
		});
		tableView.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (lastSelectedRow.get() != null) {
					Bounds boundsOfSelectedRow = lastSelectedRow.get()
							.localToScene(lastSelectedRow.get().getLayoutBounds());
					if (boundsOfSelectedRow.contains(event.getSceneX(), event.getSceneY()) == false) {
						tableView.getSelectionModel().clearSelection();
					}
				}
			}
		});

		/* Make view real only */
		((AnchorPane) buttonBar.getParent()).getChildren().remove(buttonBar);

		viewSwitch.setOnMouseClicked(e-> {
			Main.calendarView = true;
			Main.homePageController.showAppointmentView();
		});
	}

	/**
	 * Fills all text fields to show details about the appointment. If the specified
	 * appointment is null, all text fields are cleared.
	 * 
	 * @param Appointment the appointment or null
	 */
	private void showAppointmentDetails(Appointment appointment) {
		if (appointment != null) {
			// Fill the labels with info from the appointment object.
			patientIDLbl.setText(appointment.getPatientID());
			appointIDLbl.setText(appointment.getId());
			doctorIDLbl.setText(appointment.getDoctorID());
			dateLbl.setText(DateTimeUtil.formatExpand(appointment.getDate()));

		} else {
			// appointment is null, remove all the text.
			patientIDLbl.setText("");
			appointIDLbl.setText("");
			doctorIDLbl.setText("");
			dateLbl.setText("");
		}
	}

	public boolean showAppointmentDialog(Appointment appointment, String header) {
		try {
			// Load the fxml file and create a new stage for the popup dialog.
			FXMLLoader loader = new FXMLLoader(getClass().getResource("AppointmentDialog.fxml"));
			VBox aPane = loader.load();

			// Create the dialog Stage.
			Stage dialogStage = new Stage();
			dialogStage.setTitle(header);
			dialogStage.initModality(Modality.WINDOW_MODAL);
			dialogStage.initStyle(StageStyle.TRANSPARENT);
			dialogStage.initOwner(Main.stage);
			Scene scene = new Scene(aPane);
			scene.setFill(Color.TRANSPARENT);
			dialogStage.setScene(scene);

			// Set the appointment into the controller.
			AppointmentDialogController controller = loader.getController();
			controller.setDialogStage(dialogStage);
			controller.setAppointment(appointment);
			controller.setHeader(header);

			/* Set the position of the stage */
			dialogStage.setX(100);
			dialogStage.setY(190);

			overlay.toFront();
			// Show the dialog and wait until the user closes it
			dialogStage.showAndWait();

			overlay.toBack();

			return controller.isOkClicked();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	// Event Listener on Button.onAction
	@FXML
	public void handleEdit(ActionEvent event) {
		Appointment appointment = tableView.getSelectionModel().getSelectedItem();
		if (appointment != null) {
			boolean okClicked = showAppointmentDialog(appointment, "Edit Appointment");
			if (okClicked) {
				if (AppointmentSql.updateAppointment(appointment) > 0) {
					showAppointmentDetails(appointment);
				}

			}
		} else {
			// Nothing selected.
			Alert alert = new Alert(AlertType.WARNING);
			alert.initOwner(Main.stage);
			alert.setTitle("No Selection");
			alert.setHeaderText("No Appointment Selected");
			alert.setContentText("Please select a Appointment in the table.");

			alert.showAndWait();
		}
	}

	// Event Listener on Button.onAction
	@FXML
	public void handleDelete(ActionEvent event) {
		Appointment deletedAppointment = tableView.getSelectionModel().getSelectedItem();
		if (deletedAppointment != null) {
			if (AppointmentSql.removeAppointment(deletedAppointment) > 0) {
				int visibleIndex = tableView.getSelectionModel().getSelectedIndex();
				int sourceIndex = sortedList.getSourceIndexFor(observableList, visibleIndex);
				observableList.remove(sourceIndex);
			}

		} else {
			// Nothing selected.
			Alert alert = new Alert(AlertType.WARNING);
			alert.initOwner(Main.stage);
			alert.setTitle("No Selection");
			alert.setHeaderText("No Appointment Selected");
			alert.setContentText("Please select a Appointment in the table.");

			alert.showAndWait();
		}
	}

	// Event Listener on Button.onAction
	@FXML
	public void handleAdd(ActionEvent event) {
		Appointment appointment = new Appointment();
		if (showAppointmentDialog(appointment, "Add Appointment")) {
			if (AppointmentSql.addAppointment(appointment) > 0) {
				String appointID = AppointmentSql.getIdOfLastAppointment();
				if (appointID != null && !appointID.equals("")) {
					appointment.setId(appointID);
				}
				observableList.add(appointment);
				showAppointmentDetails(appointment);
			}
		}
	}

	/**
	 * Create {@link TableCell} for the PatientID column
	 * 
	 * @return the created {@link TableCell}
	 */
	public TableCell<Appointment, String> patientCellFactory() {
		TableCell<Appointment, String> cell = new TableCell<Appointment, String>() {
			@Override
			public void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				this.setText(empty ? "" : item);
			}
		};
		cell.setOnMouseClicked(e -> {
			if (!cell.isEmpty() && e.getClickCount() == 2) {
				String userId = cell.getItem();
				FXMLLoader loader = new FXMLLoader(getClass().getResource("../patient/PatientDetails.fxml"));
				Patient patient = Main.patientOverviewController.getPatient(userId);
				PatientDetailsController controller = new PatientDetailsController(patient);
				loader.setController(controller);

				AnchorPane pane = null;
				try {
					pane = loader.load();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				Stage dialogStage = new Stage();
				dialogStage.initStyle(StageStyle.TRANSPARENT);
				dialogStage.initOwner(Main.stage);
				dialogStage.focusedProperty().addListener((obv, wasFocused, isNowFocused) -> {
					if (!isNowFocused)
						dialogStage.close();
				});

				Scene scene = new Scene(pane);
				scene.setFill(Color.TRANSPARENT);
				scene.setOnKeyPressed(event -> {
					dialogStage.close();
				});
				scene.setOnMouseClicked(event -> {
					dialogStage.close();
				});
				dialogStage.setScene(scene);

				overlay.toFront();
				dialogStage.showAndWait();
				overlay.toBack();
			}
		});
		return cell;
	}

	/**
	 * Create {@link TableCell} for the DoctorID column
	 * 
	 * @return the created {@link TableCell}
	 */
	public TableCell<Appointment, String> doctorCellFactory() {
		TableCell<Appointment, String> cell = new TableCell<Appointment, String>() {
			@Override
			public void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				this.setText(empty ? "" : item);
			}
		};
		cell.setOnMouseClicked(e -> {
			if (!cell.isEmpty() && e.getClickCount() == 2) {
				String userId = cell.getItem();
				FXMLLoader loader = new FXMLLoader(getClass().getResource("../doctor/DoctorDetails.fxml"));
				Doctor doctor = Main.doctorOverviewController.getDoctor(userId);
				DoctorDetailsController controller = new DoctorDetailsController(doctor);
				loader.setController(controller);

				AnchorPane pane = null;
				try {
					pane = loader.load();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				Stage dialogStage = new Stage();
				dialogStage.initStyle(StageStyle.TRANSPARENT);
				dialogStage.initOwner(Main.stage);
				dialogStage.focusedProperty().addListener((obv, wasFocused, isNowFocused) -> {
					if (!isNowFocused)
						dialogStage.close();
				});

				Scene scene = new Scene(pane);
				scene.setFill(Color.TRANSPARENT);
				scene.setOnKeyPressed(event -> {
					dialogStage.close();
				});
				scene.setOnMouseClicked(event -> {
					dialogStage.close();
				});
				dialogStage.setScene(scene);

				overlay.toFront();
				dialogStage.showAndWait();
				overlay.toBack();
			}
		});
		return cell;
	}

	/**
	 * Create {@link TableCell} for the ID column.
	 * 
	 * @return the created {@link TableCell}
	 */
	public TableCell<Appointment, String> idCellFactory() {
		TableCell<Appointment, String> cell = new TableCell<Appointment, String>() {
			@Override
			public void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				this.setText(empty ? "" : item);
			}
		};
		cell.setOnMouseClicked(e -> {
			if (!cell.isEmpty() && e.getClickCount() == 2) {
				// edit.fire();
			}
		});
		return cell;
	}

	/**
	 * Create {@link TableCell} for the ID column.
	 * 
	 * @return the created {@link TableCell}
	 */
	public TableCell<Appointment, LocalDateTime> dateTimeCellFactory() {
		TableCell<Appointment, LocalDateTime> cell = new TableCell<Appointment, LocalDateTime>() {
			@Override
			public void updateItem(LocalDateTime item, boolean empty) {
				super.updateItem(item, empty);
				this.setText(empty ? "" : DateTimeUtil.format(item));
			}
		};
		cell.setOnMouseClicked(e -> {
			if (!cell.isEmpty() && e.getClickCount() == 2) {
				// edit.fire();
			}
		});
		return cell;
	}

	/**
	 * Chear selected item in the table.
	 */
	public void clearSelection() {
		tableView.getSelectionModel().clearSelection();
	}

	/**
	 * Set focus {@link #filterTF} on opening.
	 */
	public void setFocus() {
		filterTF.requestFocus();
	}

	/**
	 * Set text of {@link #filterTF}.
	 */
	public void selectAppointment(String filterText) {
		filterTF.setText(filterText);
		tableView.getSelectionModel().select(0);
	}

	/**
	 * Clear text of {@link #filterTF}.
	 */
	public void clearFilter() {
		filterTF.clear();
	}

	/**
	 * Get {@link Appointment} from the given {@link Appointment#id} from the
	 * {@link #observableList}.
	 * 
	 * @param appointmentID the {@link Appointment#id} of the Appointment
	 * @return The appointment object from the {@link #observableList}
	 */
	public Appointment getAppointment(String appointmentID) {
		Appointment appointment = null;
		try {
			appointment = Main.appointmentOverviewController.getSortedList().filtered(p -> {
				if (p.getId().equals(appointmentID)) {
					return true;
				}
				return false;
			}).get(0);
		} catch (IndexOutOfBoundsException e) {
		}
		return appointment;
	}
}
