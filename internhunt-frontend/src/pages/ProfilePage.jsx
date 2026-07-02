import { useState, useEffect } from 'react'
import { api } from '../api'

const PROFICIENCY_LABELS = { 1: 'Beginner', 2: 'Intermediate', 3: 'Advanced' }
const PROFICIENCY_COLORS = { 1: 'var(--muted)', 2: 'var(--yellow)', 3: 'var(--green)' }

export default function ProfilePage({ onClose }) {
  const [users, setUsers] = useState([])
  const [selectedUser, setSelectedUser] = useState(null)
  const [form, setForm] = useState(emptyForm())
  const [skills, setSkills] = useState([])       // master skill list
  const [userSkills, setUserSkills] = useState([]) // this user's skills
  const [allSkills, setAllSkills] = useState([])
  const [saving, setSaving] = useState(false)
  const [view, setView] = useState('select')      // 'select' | 'edit' | 'skills'
  const [skillSearch, setSkillSearch] = useState('')
  const [msg, setMsg] = useState(null)

  useEffect(() => {
    api.get('/users').then(setUsers).catch(() => {})
    api.get('/skills').then(setAllSkills).catch(() => {})
  }, [])

  function emptyForm() {
    return { name: '', email: '', phone: '', college: '', graduationYear: '', resumeUrl: '', githubUrl: '', linkedinUrl: '' }
  }

  function loadUser(user) {
    setSelectedUser(user)
    setForm({
      name: user.name || '', email: user.email || '', phone: user.phone || '',
      college: user.college || '', graduationYear: user.graduationYear || '',
      resumeUrl: user.resumeUrl || '', githubUrl: user.githubUrl || '', linkedinUrl: user.linkedinUrl || '',
    })
    api.get(`/users/${user.id}/skills`).then(setUserSkills).catch(() => {})
    setView('edit')
  }

  async function saveProfile() {
    setSaving(true)
    try {
      const payload = { ...form, graduationYear: form.graduationYear ? parseInt(form.graduationYear) : null }
      if (selectedUser) {
        const updated = await api.put(`/users/${selectedUser.id}`, payload)
        setSelectedUser(updated)
        setUsers(us => us.map(u => u.id === updated.id ? updated : u))
      } else {
        const created = await api.post('/users', payload)
        setUsers(us => [...us, created])
        setSelectedUser(created)
        api.get(`/users/${created.id}/skills`).then(setUserSkills).catch(() => {})
      }
      flash('✅ Profile saved!')
      setView('skills')
    } catch (e) {
      flash('❌ Failed to save: ' + e.message)
    }
    setSaving(false)
  }

  async function addSkill(skillId) {
    try {
      const added = await api.post(`/users/${selectedUser.id}/skills`, { skillId, proficiency: 1 })
      setUserSkills(us => [...us, added])
    } catch (e) { flash('❌ ' + e.message) }
  }

  async function removeSkill(skillId) {
    try {
      await api.delete(`/users/${selectedUser.id}/skills/${skillId}`)
      const refreshed = await api.get(`/users/${selectedUser.id}/skills`)
      setUserSkills([...refreshed])
    } catch (e) { flash('❌ ' + e.message) }
  }

  async function updateProficiency(skillId, proficiency) {
    try {
      const updated = await api.put(`/users/${selectedUser.id}/skills/${skillId}`, { proficiency })
      setUserSkills(us => us.map(u => u.skill.id === skillId ? updated : u))
    } catch (e) { flash('❌ ' + e.message) }
  }

  function flash(message) {
    setMsg(message)
    setTimeout(() => setMsg(null), 3000)
  }

  const userSkillIds = new Set(userSkills.map(us => us.skill.id))
  const filteredSkills = allSkills.filter(s =>
    s.name.toLowerCase().includes(skillSearch.toLowerCase())
  )

  // Group skills by category
  const grouped = filteredSkills.reduce((acc, s) => {
    const cat = s.category || 'other'
    if (!acc[cat]) acc[cat] = []
    acc[cat].push(s)
    return acc
  }, {})

  return (
    <div style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      zIndex: 200, padding: 24,
    }} onClick={onClose}>
      <div style={{
        background: 'var(--bg2)', border: '1px solid var(--border)',
        borderRadius: 16, width: '100%', maxWidth: 680,
        maxHeight: '90vh', overflow: 'hidden', display: 'flex', flexDirection: 'column',
      }} onClick={e => e.stopPropagation()}>

        {/* Header */}
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '20px 28px', borderBottom: '1px solid var(--border)',
        }}>
          <div style={{ fontFamily: 'var(--font-head)', fontWeight: 700, fontSize: '1.15rem' }}>
            {view === 'select' ? '👤 Profile' : view === 'edit' ? (selectedUser ? '✏️ Edit Profile' : '✨ Create Profile') : '🛠 My Skills'}
          </div>
          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            {view === 'edit' && selectedUser && (
              <button onClick={() => setView('skills')} style={btnStyle('var(--accent)')}>Manage Skills</button>
            )}
            {view !== 'select' && (
              <button onClick={() => setView('select')} style={btnStyle('var(--muted)')}>← Back</button>
            )}
            <button onClick={onClose} style={{ background: 'none', color: 'var(--muted)', fontSize: '1.3rem', border: 'none', cursor: 'pointer' }}>✕</button>
          </div>
        </div>

        {/* Flash message */}
        {msg && (
          <div style={{
            padding: '10px 28px', background: msg.startsWith('✅') ? '#1a2a1a' : '#2a1a1a',
            color: msg.startsWith('✅') ? 'var(--green)' : '#f87171', fontSize: '0.88rem',
          }}>{msg}</div>
        )}

        <div style={{ overflowY: 'auto', flex: 1 }}>

          {/* VIEW: Select user */}
          {view === 'select' && (
            <div style={{ padding: 28 }}>
              <p style={{ color: 'var(--muted)', fontSize: '0.88rem', marginBottom: 20 }}>
                InternHunt uses your profile to match and generate cover letters. Add your skills to get better matches.
              </p>
              {users.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '30px 0' }}>
                  <div style={{ fontSize: '2.5rem', marginBottom: 12 }}>👤</div>
                  <p style={{ color: 'var(--muted)', marginBottom: 20 }}>No profile yet. Create one to unlock AI matching.</p>
                  <button onClick={() => { setSelectedUser(null); setForm(emptyForm()); setView('edit') }}
                    style={btnStyle('var(--accent)', true)}>Create Profile</button>
                </div>
              ) : (
                <>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 20 }}>
                    {users.map(u => (
                      <div key={u.id} onClick={() => loadUser(u)} style={{
                        background: 'var(--bg3)', border: '1px solid var(--border)',
                        borderRadius: 10, padding: '14px 18px', cursor: 'pointer',
                        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                        transition: 'border-color 0.15s',
                      }}
                        onMouseEnter={e => e.currentTarget.style.borderColor = 'var(--accent)'}
                        onMouseLeave={e => e.currentTarget.style.borderColor = 'var(--border)'}
                      >
                        <div>
                          <div style={{ fontWeight: 600 }}>{u.name}</div>
                          <div style={{ color: 'var(--muted)', fontSize: '0.82rem' }}>{u.email} · {u.college || 'College not set'}</div>
                        </div>
                        <span style={{ color: 'var(--accent)', fontSize: '0.82rem' }}>Edit →</span>
                      </div>
                    ))}
                  </div>
                  <button onClick={() => { setSelectedUser(null); setForm(emptyForm()); setView('edit') }}
                    style={btnStyle('var(--accent)')}>+ New Profile</button>
                </>
              )}
            </div>
          )}

          {/* VIEW: Edit profile form */}
          {view === 'edit' && (
            <div style={{ padding: 28 }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
                <Field label="Full Name *" value={form.name} onChange={v => setForm(f => ({...f, name: v}))} placeholder="Your name" />
                <Field label="Email *" value={form.email} onChange={v => setForm(f => ({...f, email: v}))} placeholder="you@email.com" type="email" />
                <Field label="Phone" value={form.phone} onChange={v => setForm(f => ({...f, phone: v}))} placeholder="+91 XXXXX XXXXX" />
                <Field label="College" value={form.college} onChange={v => setForm(f => ({...f, college: v}))} placeholder="Your college" />
                <Field label="Graduation Year" value={form.graduationYear} onChange={v => setForm(f => ({...f, graduationYear: v}))} placeholder="2026" type="number" />
                <Field label="GitHub URL" value={form.githubUrl} onChange={v => setForm(f => ({...f, githubUrl: v}))} placeholder="https://github.com/..." />
                <Field label="LinkedIn URL" value={form.linkedinUrl} onChange={v => setForm(f => ({...f, linkedinUrl: v}))} placeholder="https://linkedin.com/in/..." />
                <Field label="Resume URL" value={form.resumeUrl} onChange={v => setForm(f => ({...f, resumeUrl: v}))} placeholder="https://drive.google.com/..." />
              </div>
              <div style={{ marginTop: 24, display: 'flex', gap: 12 }}>
                <button onClick={saveProfile} disabled={saving || !form.name || !form.email}
                  style={btnStyle('var(--accent)', true)}>
                  {saving ? 'Saving...' : selectedUser ? 'Save Changes' : 'Create Profile'}
                </button>
              </div>
            </div>
          )}

          {/* VIEW: Skills */}
          {view === 'skills' && selectedUser && (
            <div style={{ padding: 28 }}>

              {/* Current skills */}
              {userSkills.length > 0 && (
                <div style={{ marginBottom: 24 }}>
                  <div style={{ fontWeight: 600, marginBottom: 12 }}>Your Skills ({userSkills.length})</div>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                    {userSkills.map(us => (
                      <div key={us.id} style={{
                        display: 'flex', alignItems: 'center', gap: 8,
                        background: 'var(--bg3)', border: '1px solid var(--border)',
                        borderRadius: 20, padding: '4px 12px 4px 4px',
                      }}>
                        <div style={{ display: 'flex', gap: 4 }}>
                          {[1,2,3].map(p => (
                            <button key={p} onClick={() => updateProficiency(us.skill.id, p)} style={{
                              width: 18, height: 18, borderRadius: '50%',
                              background: us.proficiency >= p ? PROFICIENCY_COLORS[p] : 'var(--border)',
                              border: 'none', cursor: 'pointer', padding: 0,
                            }} title={PROFICIENCY_LABELS[p]} />
                          ))}
                        </div>
                        <span style={{ fontSize: '0.82rem', color: 'var(--text)' }}>{us.skill.name}</span>
                        <button onClick={() => removeSkill(us.skill.id)}
                          style={{ background: 'none', color: 'var(--muted)', border: 'none', cursor: 'pointer', fontSize: '0.9rem', lineHeight: 1 }}>
                          ×
                        </button>
                      </div>
                    ))}
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--muted)', marginTop: 8 }}>
                    Dots: ⚪ beginner · 🟡 intermediate · 🟢 advanced — click to change
                  </div>
                </div>
              )}

              {/* Search & add skills */}
              <div style={{ fontWeight: 600, marginBottom: 12 }}>Add Skills</div>
              <input
                value={skillSearch}
                onChange={e => setSkillSearch(e.target.value)}
                placeholder="Search skills (Java, React, SQL...)"
                style={{
                  width: '100%', background: 'var(--bg3)', border: '1px solid var(--border)',
                  borderRadius: 9, padding: '8px 14px', color: 'var(--text)', fontSize: '0.88rem',
                  outline: 'none', marginBottom: 16,
                }}
              />

              {Object.entries(grouped).map(([category, catSkills]) => (
                <div key={category} style={{ marginBottom: 16 }}>
                  <div style={{ color: 'var(--muted)', fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: 8 }}>
                    {category.replace('_', ' ')}
                  </div>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 7 }}>
                    {catSkills.map(skill => {
                      const has = userSkillIds.has(skill.id)
                      return (
                        <button key={skill.id} onClick={() => !has && addSkill(skill.id)}
                          style={{
                            background: has ? 'var(--bg3)' : 'transparent',
                            border: `1px solid ${has ? 'var(--green)' : 'var(--border)'}`,
                            color: has ? 'var(--green)' : 'var(--muted)',
                            borderRadius: 20, padding: '4px 12px', fontSize: '0.78rem',
                            cursor: has ? 'default' : 'pointer',
                            transition: 'all 0.15s',
                          }}>
                          {has ? '✓ ' : '+ '}{skill.name}
                        </button>
                      )
                    })}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function Field({ label, value, onChange, placeholder, type = 'text' }) {
  return (
    <div>
      <label style={{ display: 'block', color: 'var(--muted)', fontSize: '0.75rem', marginBottom: 5, textTransform: 'uppercase', letterSpacing: '0.05em' }}>{label}</label>
      <input type={type} value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder} style={{
        width: '100%', background: 'var(--bg3)', border: '1px solid var(--border)',
        borderRadius: 8, padding: '8px 12px', color: 'var(--text)', fontSize: '0.88rem',
        outline: 'none', boxSizing: 'border-box',
      }}
        onFocus={e => e.target.style.borderColor = 'var(--accent)'}
        onBlur={e => e.target.style.borderColor = 'var(--border)'}
      />
    </div>
  )
}

function btnStyle(color, primary = false) {
  return {
    background: primary ? color : 'transparent',
    color: primary ? '#fff' : color,
    border: `1px solid ${color}`,
    borderRadius: 8, padding: '8px 18px', fontSize: '0.85rem',
    fontWeight: 600, cursor: 'pointer', transition: 'all 0.15s',
  }
}


