--
-- Name: permissions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE
IF NOT EXISTS public.permissions
(
	"bit" integer NOT NULL,
	name text NOT NULL
);


ALTER TABLE public.permissions OWNER TO postgres;

--
-- Name: user_rooms; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE
IF NOT EXISTS public.user_rooms
(
	id integer NOT NULL,
	user_id integer,
	room_id integer
);


ALTER TABLE public.user_rooms OWNER TO postgres;

--
-- Name: user_rooms_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_rooms_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.user_rooms_id_seq OWNER TO postgres;

--
-- Name: user_rooms_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_rooms_id_seq
OWNED BY public.user_rooms.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE
IF NOT EXISTS public.users
(
	id integer NOT NULL,
	name character varying
(12),
	password character varying
(60),
	ips inet
	[],
    permissions integer
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.users_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.users_id_seq OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.users_id_seq
OWNED BY public.users.id;


--
-- Name: user_rooms id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_rooms
ALTER COLUMN id
SET
DEFAULT nextval
('public.user_rooms_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
ALTER COLUMN id
SET
DEFAULT nextval
('public.users_id_seq'::regclass);


--
-- Data for Name: permissions; Type: TABLE DATA; Schema: public; Owner: postgres
--

-- COPY public.permissions
--("bit", name) FROM stdin;
INSERT INTO public.permissions
    (bit, name)
VALUES(1, 'user-view');
INSERT INTO public.permissions
    (bit, name)
VALUES(2, 'user-edit');
INSERT INTO public.permissions
    (bit, name)
VALUES(4, 'play');
INSERT INTO public.permissions
    (bit, name)
VALUES(8, 'group-all');
INSERT INTO public.permissions
    (bit, name)
VALUES(1, 'group-view');


--
-- Name: user_rooms_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.user_rooms_id_seq', 1, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_id_seq', 1, true);


--
-- Name: permissions permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.permissions
ADD CONSTRAINT permissions_pkey PRIMARY KEY
("bit");


--
-- Name: user_rooms user_rooms_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_rooms
ADD CONSTRAINT user_rooms_pkey PRIMARY KEY
(id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
ADD CONSTRAINT users_pkey PRIMARY KEY
(id);


--
-- Name: user_rooms user_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_rooms
ADD CONSTRAINT user_id FOREIGN KEY
(user_id) REFERENCES public.users
(id);


--
-- PostgreSQL database dump complete
--