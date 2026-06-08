/*********************************************
 * OPL 22.1.2.0 Model
 * Revised according to manuscript model
 * Revision date: 2026.05.25
 *********************************************/

// =====================================================
// Sets definition
// =====================================================
{string} Nodes = ...;
{string} Customers = ...;
{string} Stations = ...;
{string} BusOrigins = ...;
{string} BusDestinations = ...;
{string} DistributionCenter = ...;
{string} BusRoutes = ...;
{string} LogisticsVehicles = ...;

int Stages = ...;
range T = 1..Stages;

//2026.05.25璋冩暣锛氭柊澧炴墿灞曟椂闂撮泦鍚堬紝鐢ㄤ簬椤惧鎻愬墠閫佽揪浣欓 I_{i,t+1}
range Tplus = 1..(Stages + 1);

{string} LogisticsNodes = ...;
{string} NonLogisticsNodes = ...;

tuple Arc {
    string i;
    string j;
}

{Arc} Arcs = ...;
{Arc} BusRouteArcs[BusRoutes] = ...;


// =====================================================
// Parameters
// =====================================================
float Distance[Arcs] = ...;
float TransportCost[Arcs] = ...;

float StationLeaseCost[Nodes][T] = ...;

//2026.05.25璋冩暣锛氭鏂囩洰鏍囧嚱鏁颁笉鍐嶅寘鍚湇鍔＄珯搴撳瓨鎸佹湁鎴愭湰椤癸紝鏁呭垹闄� StationInventoryHoldingCost 鍙傛暟
//float StationInventoryHoldingCost[Stations][T] = ...;

float BusServiceCost[BusRoutes][Nodes] = ...;

float Demand_n[Customers][T] = ...;
float Demand_s[Customers][T] = ...;

float BusCapacity[BusRoutes] = ...;
float VehicleCapacity[LogisticsVehicles] = ...;

float TravelTime[Arcs] = ...;
float ServiceTime[Nodes] = ...;
float MaxWorkingTime = ...;

float M = 10000;
float eps = 0.001;


// =====================================================
// Decision variables
// =====================================================

// Bus-route arc decision
dvar boolean x[BusRoutes][Arcs][T];

// Service-station opening decision
dvar boolean h[Nodes][T];

// Logistics-vehicle arc decision
dvar boolean u[LogisticsVehicles][Arcs][T];

// Logistics vehicle replenishes / visits station
dvar boolean w[LogisticsVehicles][Nodes][T];

// Bus picks up goods at station
dvar boolean v[BusRoutes][Nodes][T];

// Service-mode decision variables
dvar boolean BusServeCustomer[BusRoutes][Customers][T];
dvar boolean VehicleServeCustomer_n[LogisticsVehicles][Customers][T];
dvar boolean VehicleServeCustomer_s[LogisticsVehicles][Customers][T];

// Delivery quantities
dvar float+ y_n[BusRoutes][Customers][T];
dvar float+ z_n[LogisticsVehicles][Customers][T];
dvar float+ z_s[LogisticsVehicles][Customers][T];

// Station replenishment and pickup quantities
dvar float+ q_n[LogisticsVehicles][Nodes][T];
dvar float+ b_n[BusRoutes][Nodes][T];

//2026.05.25璋冩暣锛氭湇鍔＄珯鏅�氳揣鐗╄法鏈熷簱瀛樺彉閲忥紝瀵瑰簲姝ｆ枃 I_{jt}^{n,S}
dvar float+ StationInventory_n[Stations][T];

//2026.05.25璋冩暣锛氶【瀹㈡彁鍓嶉�佽揪浣欓鍙橀噺锛屽搴旀鏂� I_{it}^{n}, I_{it}^{s}
dvar float+ CustomerAdvance_n[Customers][Tplus];
dvar float+ CustomerAdvance_s[Customers][Tplus];

// Bus load variables
dvar float+ BusLoad[BusRoutes][Arcs][T];
dvar float+ BusLoadStart[BusRoutes][T];
dvar float+ BusPickup[BusRoutes][Nodes][T];
dvar float+ BusDelivery[BusRoutes][Nodes][T];
dvar float+ BusLoadAtNode[BusRoutes][Nodes][T];

// Logistics vehicle load variables
dvar float+ VehicleLoadStart[LogisticsVehicles][T];
dvar float+ TotalVehicleLoad[LogisticsVehicles][T];

// Auxiliary variables
dvar boolean StationUsed[Nodes][T];
dvar float+ mtz_order[LogisticsVehicles][Nodes][T];


// =====================================================
// Objective function, corresponding to manuscript Eq. (1)
// =====================================================
minimize
    // Service-station leasing cost
    sum(j in Stations, t in T)
        StationLeaseCost[j][t] * h[j][t]
        +

    // Bus-based service cost for ordinary goods
    sum(t in T, r in BusRoutes, i in Customers)
        BusServiceCost[r][i] * y_n[r][i][t]

    +

    // Logistics-vehicle transportation cost
    sum(t in T, k in LogisticsVehicles, arc in Arcs)
        TransportCost[arc] * Distance[arc] * u[k][arc][t] ;

    //2026.05.25璋冩暣锛氬垹闄ゆ湇鍔＄珯搴撳瓨鎸佹湁鎴愭湰椤广��
    //鏈嶅姟绔欑煭鏈熸殏瀛樺拰浜ゆ帴鍔熻兘鐢辨湇鍔＄珯绉熻祦鎴愭湰 StationLeaseCost 瑕嗙洊銆�


// =====================================================
// Constraints
// =====================================================
subject to {

    // -------------------------------------------------
    // Constraint (2): Non-station nodes cannot be opened as service stations
    // -------------------------------------------------
    forall(j in Nodes, t in T: !(j in Stations)) {
        h[j][t] == 0;
        StationUsed[j][t] == 0;
    }


    // -------------------------------------------------
    // Constraint (3): Station-related quantities can only be defined on station nodes
    // -------------------------------------------------
    forall(k in LogisticsVehicles, j in Nodes, t in T: !(j in Stations)) {
        q_n[k][j][t] == 0;
        w[k][j][t] == 0;
    }

    forall(r in BusRoutes, j in Nodes, t in T: !(j in Stations)) {
        b_n[r][j][t] == 0;
        v[r][j][t] == 0;
    }


    // -------------------------------------------------
    // Constraint (4): Bus-route variables are restricted to fixed bus-route arcs
    // -------------------------------------------------
    forall(r in BusRoutes, arc in Arcs, t in T: !(arc in BusRouteArcs[r])) {
        x[r][arc][t] == 0;
        BusLoad[r][arc][t] == 0;
    }


    // -------------------------------------------------
    // Constraint (5): Fixed bus route starts from its origin
    // -------------------------------------------------
    forall(r in BusRoutes, t in T) {
        sum(arc in BusRouteArcs[r]: arc.i in BusOrigins) x[r][arc][t] == 1;
    }


    // -------------------------------------------------
    // Constraint (6): Fixed bus route ends at its destination
    // -------------------------------------------------
    forall(r in BusRoutes, t in T) {
        sum(arc in BusRouteArcs[r]: arc.j in BusDestinations) x[r][arc][t] == 1;
    }


    // -------------------------------------------------
    // Constraint (7): Flow conservation on fixed bus routes
    // -------------------------------------------------
    forall(r in BusRoutes, t in T, j in Nodes) {
        if (!(j in BusOrigins) && !(j in BusDestinations)) {
            sum(arc in BusRouteArcs[r]: arc.j == j) x[r][arc][t]
            ==
            sum(arc in BusRouteArcs[r]: arc.i == j) x[r][arc][t];
        }
    }


    // -------------------------------------------------
    // Constraint (8): Initial freight load of scheduled buses
    // -------------------------------------------------
    //2026.05.25璋冩暣锛氬叕浜よ溅鍙互浠庤捣鐐规惡甯﹁揣鐗╁嚭鍙戙��
    //鍒濆杞借揣閲� = 鍏氦鏈湡閰嶉�侀噺 - 鏈湡浠庢湇鍔＄珯鍙栬揣閲忋��
    forall(r in BusRoutes, t in T) {
        BusLoadStart[r][t]
        ==
        sum(i in Customers) y_n[r][i][t]
        -
        sum(j in Stations) b_n[r][j][t];
    }


    // -------------------------------------------------
    // Constraint (9): Freight load on the first bus arc
    // -------------------------------------------------
    //2026.05.25璋冩暣锛氬叕浜よ捣鐐瑰彂鍑哄姬涓婄殑杞借揣閲忕瓑浜庡叕浜ゅ垵濮嬭浇璐ч噺銆�
    forall(r in BusRoutes, t in T) {
        sum(arc in BusRouteArcs[r]: arc.i in BusOrigins) BusLoad[r][arc][t]
        ==
        BusLoadStart[r][t];
    }


    // -------------------------------------------------
    // Constraint (10): Bus capacity on each bus-route arc
    // -------------------------------------------------
    forall(r in BusRoutes, t in T) {
        BusLoadStart[r][t] <= BusCapacity[r];

        forall(arc in BusRouteArcs[r]) {
            BusLoad[r][arc][t] <= BusCapacity[r] * x[r][arc][t];
        }
    }


    // -------------------------------------------------
    // Constraint (11): Bus load balance at customer nodes
    // -------------------------------------------------
    forall(r in BusRoutes, t in T, j in Customers) {
        sum(arc_in in BusRouteArcs[r]: arc_in.j == j) BusLoad[r][arc_in][t]
        ==
        sum(arc_out in BusRouteArcs[r]: arc_out.i == j) BusLoad[r][arc_out][t]
        +
        y_n[r][j][t];
    }


    // -------------------------------------------------
    // Constraint (12): Bus load balance at service stations
    // -------------------------------------------------
    forall(r in BusRoutes, t in T, j in Stations) {
        sum(arc_in in BusRouteArcs[r]: arc_in.j == j) BusLoad[r][arc_in][t]
        +
        b_n[r][j][t]
        ==
        sum(arc_out in BusRouteArcs[r]: arc_out.i == j) BusLoad[r][arc_out][t];
    }


    // -------------------------------------------------
    // Constraint (13): Bus load at destination is zero
    // -------------------------------------------------
    forall(r in BusRoutes, t in T) {
        sum(arc in BusRouteArcs[r]: arc.j in BusDestinations) BusLoad[r][arc][t] == 0;
    }


    // -------------------------------------------------
    // Constraint (14): Bus capacity after station pickup
    // -------------------------------------------------
    forall(r in BusRoutes, t in T, j in Stations) {
        sum(arc_in in BusRouteArcs[r]: arc_in.j == j) BusLoad[r][arc_in][t]
        +
        b_n[r][j][t]
        <=
        BusCapacity[r];
    }


    // -------------------------------------------------
    // Constraint (15): Bus delivery can occur only on fixed bus routes
    // -------------------------------------------------
    forall(r in BusRoutes, t in T, i in Customers) {
        BusServeCustomer[r][i][t]
        <=
        sum(arc in BusRouteArcs[r]: arc.j == i) x[r][arc][t];

        y_n[r][i][t]
        <=
        M * sum(arc in BusRouteArcs[r]: arc.j == i) x[r][arc][t];
    }


    // -------------------------------------------------
    // Constraint (16): Bus pickup can occur only at opened service stations on the fixed route
    // -------------------------------------------------
    forall(r in BusRoutes, t in T, j in Stations) {
        b_n[r][j][t] <= M * v[r][j][t];

        v[r][j][t] <= sum(arc in BusRouteArcs[r]: arc.j == j) x[r][arc][t];
        v[r][j][t] <= h[j][t];

        h[j][t] >= v[r][j][t];
    }


    // -------------------------------------------------
    // Constraint (17): Bus pickup and delivery records
    // -------------------------------------------------
    forall(r in BusRoutes, t in T) {
        forall(j in Stations) {
            BusPickup[r][j][t] == b_n[r][j][t];
        }

        forall(i in Customers) {
            BusDelivery[r][i][t] == y_n[r][i][t];
        }
    }


    // -------------------------------------------------
    // Constraint (18): Bus load record at each node
    // -------------------------------------------------
    forall(r in BusRoutes, t in T, j in Nodes) {
        if (j in BusOrigins) {
            BusLoadAtNode[r][j][t] == BusLoadStart[r][t];
        }
        else if (j in Customers) {
            BusLoadAtNode[r][j][t]
            ==
            sum(arc_in in BusRouteArcs[r]: arc_in.j == j) BusLoad[r][arc_in][t]
            -
            y_n[r][j][t];
        }
        else if (j in Stations) {
            BusLoadAtNode[r][j][t]
            ==
            sum(arc_in in BusRouteArcs[r]: arc_in.j == j) BusLoad[r][arc_in][t]
            +
            b_n[r][j][t];
        }
        else {
            BusLoadAtNode[r][j][t]
            ==
            sum(arc_in in BusRouteArcs[r]: arc_in.j == j) BusLoad[r][arc_in][t];
        }
    }


    // -------------------------------------------------
    // Constraint (19): Logistics vehicle route flow balance
    // -------------------------------------------------
    forall(k in LogisticsVehicles, t in T, j in LogisticsNodes) {
        if (j != "DC") {
            sum(arc in Arcs: arc.j == j) u[k][arc][t]
            ==
            sum(arc in Arcs: arc.i == j) u[k][arc][t];
        }
    }


    // -------------------------------------------------
    // Constraint (20): Logistics vehicle departs from and returns to the distribution center
    // -------------------------------------------------
    forall(k in LogisticsVehicles, t in T) {
        sum(arc in Arcs: arc.i == "DC") u[k][arc][t] <= 1;
        sum(arc in Arcs: arc.j == "DC") u[k][arc][t] <= 1;

        sum(arc in Arcs: arc.i == "DC") u[k][arc][t]
        ==
        sum(arc in Arcs: arc.j == "DC") u[k][arc][t];
    }


    // -------------------------------------------------
    // Constraint (21): Logistics vehicles cannot visit non-logistics nodes
    // -------------------------------------------------
    forall(k in LogisticsVehicles, t in T, j in NonLogisticsNodes) {
        sum(arc in Arcs: arc.j == j) u[k][arc][t] == 0;
        sum(arc in Arcs: arc.i == j) u[k][arc][t] == 0;
    }


    // -------------------------------------------------
    // Constraint (22): Logistics vehicle delivery requires visiting the customer
    // -------------------------------------------------
    forall(k in LogisticsVehicles, t in T, i in Customers) {
        VehicleServeCustomer_n[k][i][t]
        <=
        sum(arc in Arcs: arc.j == i) u[k][arc][t];

        VehicleServeCustomer_s[k][i][t]
        <=
        sum(arc in Arcs: arc.j == i) u[k][arc][t];

        z_n[k][i][t] <= M * sum(arc in Arcs: arc.j == i) u[k][arc][t];
        z_s[k][i][t] <= M * sum(arc in Arcs: arc.j == i) u[k][arc][t];
    }


    // -------------------------------------------------
    // Constraint (23): Logistics vehicle replenishment requires visiting an opened service station
    // -------------------------------------------------
    forall(k in LogisticsVehicles, t in T, j in Stations) {
        q_n[k][j][t] <= M * h[j][t];
        q_n[k][j][t] <= M * w[k][j][t];

        w[k][j][t]
        <=
        sum(arc in Arcs: arc.j == j) u[k][arc][t];
    }


    // -------------------------------------------------
    // Constraint (24): Each service station is replenished by at most one logistics vehicle in each period
    // -------------------------------------------------
    forall(j in Stations, t in T) {
        sum(k in LogisticsVehicles) w[k][j][t] <= 1;
    }


    // -------------------------------------------------
    // Constraint (25): Logistics vehicle load initialization
    // -------------------------------------------------
    forall(k in LogisticsVehicles, t in T) {
        VehicleLoadStart[k][t]
        ==
        sum(i in Customers) (z_n[k][i][t] + z_s[k][i][t])
        +
        sum(j in Stations) q_n[k][j][t];

        VehicleLoadStart[k][t] <= VehicleCapacity[k];
    }


    // -------------------------------------------------
    // Constraint (26): Logistics vehicle total capacity
    // -------------------------------------------------
    forall(k in LogisticsVehicles, t in T) {
        TotalVehicleLoad[k][t]
        ==
        sum(i in Customers) (z_n[k][i][t] + z_s[k][i][t])
        +
        sum(j in Stations) q_n[k][j][t];

        TotalVehicleLoad[k][t] <= VehicleCapacity[k];
    }


    // -------------------------------------------------
    // Constraint (27): Service-station inter-period inventory balance
    // -------------------------------------------------
    //2026.05.25璋冩暣锛氭湇鍔＄珯鏅�氳揣鐗╁厑璁歌法鏈熸殏瀛橈紝鍒濆搴撳瓨榛樿涓��
    forall(j in Stations) {
        StationInventory_n[j][1]
        ==
        sum(k in LogisticsVehicles) q_n[k][j][1]
        -
        sum(r in BusRoutes) b_n[r][j][1];
    }

    forall(j in Stations, t in T: t > 1) {
        StationInventory_n[j][t]
        ==
        StationInventory_n[j][t-1]
        +
        sum(k in LogisticsVehicles) q_n[k][j][t]
        -
        sum(r in BusRoutes) b_n[r][j][t];
    }

    forall(j in Stations, t in T) {
        StationInventory_n[j][t] <= M * h[j][t];
    }


    // -------------------------------------------------
    // Constraint (28a)-(28c): Delivery quantity and service-decision consistency
    // -------------------------------------------------
    //2026.05.25璋冩暣锛氬師妯″瀷涓湇鍔″彉閲忎笌浜や粯閲忕己灏戠洿鎺ョ粦瀹氥��
    //姝ゅ灏� y_n, z_n, z_s 鍒嗗埆涓� BusServeCustomer, VehicleServeCustomer_n, VehicleServeCustomer_s 鍏宠仈銆�
    forall(r in BusRoutes, i in Customers, t in T) {
        y_n[r][i][t] <= M * BusServeCustomer[r][i][t];
    }

    forall(k in LogisticsVehicles, i in Customers, t in T) {
        z_n[k][i][t] <= M * VehicleServeCustomer_n[k][i][t];
        z_s[k][i][t] <= M * VehicleServeCustomer_s[k][i][t];
    }


    // -------------------------------------------------
    // Constraint (29): Ordinary-goods delivery is assigned to at most one service mode per period
    // -------------------------------------------------
    //2026.05.25璋冩暣锛氱敱浜庨【瀹㈤渶姹傚厑璁告彁鍓嶉�佽揪锛屽師鈥滃綋鏈熼渶姹傚繀椤诲綋鏈熸湇鍔′竴娆♀�濈殑绛夊紡鏀逛负鈥滄瘡鏈熻嚦澶氫竴娆℃湇鍔♀�濄��
    forall(i in Customers, t in T) {
        sum(r in BusRoutes) BusServeCustomer[r][i][t]
        +
        sum(k in LogisticsVehicles) VehicleServeCustomer_n[k][i][t]
        <= 1;
    }


    // -------------------------------------------------
    // Constraint (30): Special-goods delivery is assigned to at most one logistics vehicle per period
    // -------------------------------------------------
    //2026.05.25璋冩暣锛氱壒娈婅揣鐗╁彧鑳界敱鐗╂祦杞︽湇鍔★紝浣嗕笉鍐嶈姹傚叾蹇呴』鍦ㄩ渶姹傚彂鐢熷綋鏈熸湇鍔°��
    forall(i in Customers, t in T) {
        sum(k in LogisticsVehicles) VehicleServeCustomer_s[k][i][t] <= 1;
    }


    // -------------------------------------------------
    // Constraint (31a): Bus delivery and special-goods logistics delivery cannot occur simultaneously
    // -------------------------------------------------
    //2026.05.25璋冩暣锛氫繚璇佸悓涓�椤惧鍚屼竴鏃舵湡鏈�澶氭帴鍙椾竴娆＄墿鐞嗚闂��
    forall(i in Customers, r in BusRoutes, k in LogisticsVehicles, t in T) {
        BusServeCustomer[r][i][t]
        +
        VehicleServeCustomer_s[k][i][t]
        <= 1;
    }


    // -------------------------------------------------
    // Constraint (31b): If ordinary and special goods are both delivered by logistics vehicles,
    //                   they must be delivered by the same logistics vehicle
    // -------------------------------------------------
    //2026.05.25璋冩暣锛氶伩鍏嶆櫘閫氳揣鐗╁拰鐗规畩璐х墿鐢变笉鍚岀墿娴佽溅鍦ㄥ悓涓�鏃舵湡鍒嗗埆璁块棶鍚屼竴椤惧銆�
    forall(i in Customers, k in LogisticsVehicles, l in LogisticsVehicles, t in T: k != l) {
        VehicleServeCustomer_n[k][i][t]
        +
        VehicleServeCustomer_s[l][i][t]
        <= 1;
    }


    // -------------------------------------------------
    // Constraint (32): Customer advance-delivery balance for ordinary goods
    // -------------------------------------------------
    //2026.05.25璋冩暣锛氶【瀹㈣妭鐐瑰厑璁告彁鍓嶉�佽揪锛孋ustomerAdvance_n 琛ㄧず鏅�氳揣鐗╂彁鍓嶆弧瓒充綑棰濄��
    forall(i in Customers, t in T) {
        CustomerAdvance_n[i][t + 1]
        ==
        CustomerAdvance_n[i][t]
        +
        sum(r in BusRoutes) y_n[r][i][t]
        +
        sum(k in LogisticsVehicles) z_n[k][i][t]
        -
        Demand_n[i][t];
    }


    // -------------------------------------------------
    // Constraint (33): Customer advance-delivery balance for special goods
    // -------------------------------------------------
    //2026.05.25璋冩暣锛欳ustomerAdvance_s 琛ㄧず鐗规畩璐х墿鎻愬墠婊¤冻浣欓銆�
    forall(i in Customers, t in T) {
        CustomerAdvance_s[i][t + 1]
        ==
        CustomerAdvance_s[i][t]
        +
        sum(k in LogisticsVehicles) z_s[k][i][t]
        -
        Demand_s[i][t];
    }


    // -------------------------------------------------
    // Constraint (34): Nonnegative customer advance-delivery surplus
    // -------------------------------------------------
    //2026.05.25璋冩暣锛氶潪璐熸彁鍓嶆弧瓒充綑棰濈敤浜庣姝㈠欢鏈�/娆犱氦銆�
    //鐢变簬 CustomerAdvance_n 鍜� CustomerAdvance_s 涓� dvar float+锛�
    //璇ラ潪璐熸�у凡鐢卞彉閲忓畾涔夎嚜鍔ㄤ繚璇侊紝杩欓噷涓嶅啀閲嶅娣诲姞鏄惧紡绾︽潫銆�


    // -------------------------------------------------
    // Constraint (35): Initial customer advance-delivery surplus
    // -------------------------------------------------
    //2026.05.25璋冩暣锛氶【瀹㈡彁鍓嶆弧瓒充綑棰濆垵濮嬪�间负0銆�
    forall(i in Customers) {
        CustomerAdvance_n[i][1] == 0;
        CustomerAdvance_s[i][1] == 0;
    }


    // -------------------------------------------------
    // Constraint (36): Logistics vehicle working-time limit
    // -------------------------------------------------
    forall(k in LogisticsVehicles, t in T) {
        sum(arc in Arcs) TravelTime[arc] * u[k][arc][t]
        +
        sum(i in Customers) ServiceTime[i] * (z_n[k][i][t] + z_s[k][i][t])
        +
        sum(j in Stations) ServiceTime[j] * q_n[k][j][t]
        <=
        MaxWorkingTime;
    }


    // -------------------------------------------------
    // Constraint (37): MTZ subtour elimination for logistics vehicles
    // -------------------------------------------------
    forall(k in LogisticsVehicles, t in T) {
        mtz_order[k]["DC"][t] == 0;
    }

    forall(k in LogisticsVehicles, t in T, j in LogisticsNodes: j != "DC") {
        mtz_order[k][j][t]
        <=
        card(LogisticsNodes) * sum(arc in Arcs: arc.j == j) u[k][arc][t];

        mtz_order[k][j][t]
        >=
        sum(arc in Arcs: arc.j == j) u[k][arc][t];
    }

    forall(k in LogisticsVehicles, t in T, arc in Arcs:
           arc.i in LogisticsNodes
           && arc.j in LogisticsNodes
           && arc.i != "DC"
           && arc.j != "DC") {
        mtz_order[k][arc.j][t]
        >=
        mtz_order[k][arc.i][t] + 1
        -
        card(LogisticsNodes) * (1 - u[k][arc][t]);
    }


    // -------------------------------------------------
    // Constraint (38): Depot activation for logistics vehicles
    // -------------------------------------------------
    forall(k in LogisticsVehicles, t in T) {
        sum(j in LogisticsNodes: j != "DC")
        sum(arc in Arcs: arc.j == j) u[k][arc][t]
        <=
        M * sum(arc in Arcs: arc.i == "DC") u[k][arc][t];
    }


    // -------------------------------------------------
    // Constraint (39): Variable domains
    // Binary domains are enforced by dvar boolean.
    // Nonnegative continuous domains are enforced by dvar float+.
    // -------------------------------------------------
}

main {
    thisOplModel.generate();

    cplex.tilim = 10800;

    cplex.solve();
}