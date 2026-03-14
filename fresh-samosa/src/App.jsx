import './App.css'

function App() {
  return (
    <>
      <header className="header">
        <div className="container header-inner">
          <h1 className="logo">Fresh <span>Samosa</span></h1>
          <nav className="nav" aria-label="Main">
            <a href="#about">About</a>
            <a href="#snacks">Snacks</a>
            <a href="#contact">Contact</a>
          </nav>
        </div>
      </header>

      <main>
        <section className="hero" aria-labelledby="hero-heading">
          <div className="container">
            <p className="hero-tag">India’s Favourite Tea-Time Snack</p>
            <h1 id="hero-heading">
              Crispy. Golden. <em>Fresh.</em>
            </h1>
            <p className="hero-desc">
              Handcrafted samosas made with the finest spices and fillings — perfect with a cup of chai any time of the day.
            </p>
            <a href="#snacks" className="hero-cta">
              See our snacks →
            </a>
          </div>
        </section>

        <section id="about" className="section about" aria-labelledby="about-heading">
          <div className="container">
            <h2 id="about-heading" className="section-title">About <span>Samosa</span></h2>
            <div className="about-inner">
              <div className="about-text">
                <p>
                  The samosa has been a beloved snack across India for centuries — a crisp, golden pastry filled with spiced potatoes, peas, and sometimes minced meat or paneer.
                </p>
                <p>
                  At Fresh Samosa we honour this tradition by making every batch by hand, using fresh ingredients and time-tested recipes. Whether it’s evening chai or a quick bite, we’re here to bring you that perfect crunch.
                </p>
              </div>
              <div className="about-visual" aria-hidden="true">
                🥟
              </div>
            </div>
          </div>
        </section>

        <section id="snacks" className="section" aria-labelledby="snacks-heading">
          <div className="container">
            <h2 id="snacks-heading" className="section-title">Tea-Time <span>Favourites</span></h2>
            <div className="snacks-grid">
              <article className="snack-card">
                <div className="snack-card-image" aria-hidden="true">🥟</div>
                <div className="snack-card-body">
                  <h3>Classic Samosa</h3>
                  <p>Spiced potato and pea filling in a crisp, flaky crust. Served with mint or tamarind chutney.</p>
                </div>
              </article>
              <article className="snack-card">
                <div className="snack-card-image" aria-hidden="true">🍵</div>
                <div className="snack-card-body">
                  <h3>Masala Chai</h3>
                  <p>Freshly brewed tea with cardamom, ginger, and milk — the perfect pair for your samosa.</p>
                </div>
              </article>
              <article className="snack-card">
                <div className="snack-card-image" aria-hidden="true">🥠</div>
                <div className="snack-card-body">
                  <h3>Paneer Samosa</h3>
                  <p>Crumbled paneer with green peas and mild spices, wrapped in our signature pastry.</p>
                </div>
              </article>
            </div>
          </div>
        </section>

        <section id="contact" className="section about" aria-labelledby="contact-heading">
          <div className="container">
            <h2 id="contact-heading" className="section-title">Visit <span>Us</span></h2>
            <p style={{ textAlign: 'center', maxWidth: '40ch', margin: '0 auto', color: 'var(--color-brown-light)' }}>
              Come by for a fresh batch any evening. We’re open daily for tea and snacks.
            </p>
          </div>
        </section>
      </main>

      <footer className="footer">
        <div className="container">
          <span className="footer-logo">Fresh <span>Samosa</span></span>
          <p className="footer-copy">© Fresh Samosa. Tea-time done right.</p>
          <a href="#about">Back to top</a>
        </div>
      </footer>
    </>
  )
}

export default App
